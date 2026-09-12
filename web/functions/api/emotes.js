import {
  json, bad, isGuild, sha256Hex, sniffType, EXT_BY_TYPE, MAX_BYTES,
  triggerWord, hamming, SIMILAR_THRESHOLD, nowSeconds, rowToEmote,
} from "../_shared.js";

/** Public: the approved pack, newest first. */
export async function onRequestGet({ env }) {
  const { results } = await env.DB.prepare(
    `SELECT * FROM emotes WHERE status = 'approved' ORDER BY created_at DESC`
  ).all();
  return json({ emotes: results.map(rowToEmote) });
}

export async function onRequestPost({ request, env }) {
  if (!isGuild(request, env)) return bad("Wrong passcode.", 401);

  const ip = request.headers.get("cf-connecting-ip") || "unknown";
  const since = nowSeconds() - 3600;
  const { count } = await env.DB.prepare(
    `SELECT COUNT(*) AS count FROM upload_log WHERE ip = ? AND at > ?`
  ).bind(ip, since).first();
  if (count >= 10) return bad("That's 10 uploads in an hour. Give it a rest.", 429);

  const form = await request.formData();
  const file = form.get("file");
  const uploader = String(form.get("uploader") || "").trim().slice(0, 40);
  const wanted = String(form.get("name") || "").trim();
  const dhash = String(form.get("dhash") || "").toLowerCase().slice(0, 16) || null;
  const animated = form.get("animated") === "true" ? 1 : 0;

  if (!(file instanceof File)) return bad("No file received.");
  if (!uploader) return bad("Add your name so we know who to thank.");
  if (file.size > MAX_BYTES) return bad("That image is over 2 MB. Trim it down first.");

  const buffer = await file.arrayBuffer();
  const type = sniffType(buffer);
  if (!type) return bad("That isn't a GIF, PNG, WebP or JPEG.");
  const ext = EXT_BY_TYPE[type];

  const name = triggerWord(wanted || file.name.replace(/\.[^.]+$/, ""));
  if (!name) return bad("That name is all characters chat breaks on — nothing usable is left.");
  if (name.length > 40) return bad("That name is too long to type in chat.");

  const sha = await sha256Hex(buffer);

  const clash = await env.DB.prepare(
    `SELECT name, status FROM emotes WHERE name = ? AND status IN ('pending','approved')`
  ).bind(name).first();
  if (clash) {
    return bad(
      clash.status === "approved"
        ? `${name} already exists in the pack.`
        : `${name} is already waiting for approval.`,
      409
    );
  }

  const dupe = await env.DB.prepare(
    `SELECT name, status FROM emotes WHERE sha256 = ? AND status IN ('pending','approved')`
  ).bind(sha).first();
  if (dupe) return bad(`That exact image is already here as ${dupe.name}.`, 409);

  // Perceptual near-duplicates are a warning, not a block: you decide at approval.
  let note = null;
  if (dhash) {
    const { results } = await env.DB.prepare(
      `SELECT name, dhash FROM emotes WHERE dhash IS NOT NULL AND status IN ('pending','approved')`
    ).all();
    const near = results.find((r) => hamming(dhash, r.dhash) <= SIMILAR_THRESHOLD);
    if (near) note = `Looks a lot like ${near.name}`;
  }

  const id = crypto.randomUUID();
  await env.BUCKET.put(`emotes/${id}.${ext}`, buffer, { httpMetadata: { contentType: type } });

  const at = nowSeconds();
  await env.DB.batch([
    env.DB.prepare(
      `INSERT INTO emotes (id, name, ext, sha256, dhash, uploader, status, note, animated, bytes, created_at)
       VALUES (?, ?, ?, ?, ?, ?, 'pending', ?, ?, ?, ?)`
    ).bind(id, name, ext, sha, dhash, uploader, note, animated, file.size, at),
    env.DB.prepare(`INSERT INTO upload_log (ip, at) VALUES (?, ?)`).bind(ip, at),
  ]);

  return json({ ok: true, id, name, note, url: `/img/${id}.${ext}` }, 201);
}
