import { json, bad, isAdmin, triggerWord, githubPutSource, githubDeleteSource, githubSourceSha, nowSeconds } from "../../_shared.js";

/** POST {action: approve|reject, name?, reason?} — the whole approval queue. */
export async function onRequestPost({ request, env, params }) {
  if (!isAdmin(request, env)) return bad("Admin passcode required.", 401);

  const row = await env.DB.prepare(`SELECT * FROM emotes WHERE id = ?`).bind(params.id).first();
  if (!row) return bad("No such emote.", 404);

  const body = await request.json().catch(() => ({}));
  const action = body.action;

  if (action === "reject") {
    if (row.status === "approved") return bad("Already in the pack — delete it instead.", 409);
    await env.DB.prepare(
      `UPDATE emotes SET status = 'rejected', note = ?, decided_at = ? WHERE id = ?`
    ).bind(String(body.reason || "").slice(0, 200) || null, nowSeconds(), row.id).run();
    return json({ ok: true, status: "rejected" });
  }

  if (action === "approve") {
    if (row.status === "approved") return json({ ok: true, status: "approved" });

    let name = row.name;
    if (body.name) {
      name = triggerWord(String(body.name).trim());
      if (!name) return bad("That rename leaves nothing chat can match.");
      const clash = await env.DB.prepare(
        `SELECT id FROM emotes WHERE name = ? AND id != ? AND status IN ('pending','approved')`
      ).bind(name, row.id).first();
      if (clash) return bad(`${name} is already taken.`, 409);
    }

    const object = await env.BUCKET.get(`emotes/${row.id}.${row.ext}`);
    if (!object) return bad("The uploaded image is missing from storage.", 500);

    let sha;
    try {
      // Naming the file after the trigger word keeps the build's naming a no-op.
      sha = await githubPutSource(
        env,
        `${name}.${row.ext}`,
        await object.arrayBuffer(),
        `Add ${name} (uploaded by ${row.uploader})`
      );
    } catch (err) {
      return bad(`Commit failed: ${err.message}`, 502);
    }

    await env.DB.prepare(
      `UPDATE emotes SET status = 'approved', name = ?, note = NULL, github_sha = ?, decided_at = ? WHERE id = ?`
    ).bind(name, sha, nowSeconds(), row.id).run();

    return json({ ok: true, status: "approved", name });
  }

  return bad("Unknown action.");
}

/** Pull an emote back out of the pack; the rebuild drops its texture too. */
export async function onRequestDelete({ request, env, params }) {
  if (!isAdmin(request, env)) return bad("Admin passcode required.", 401);

  const row = await env.DB.prepare(`SELECT * FROM emotes WHERE id = ?`).bind(params.id).first();
  if (!row) return bad("No such emote.", 404);

  if (row.status === "approved") {
    const path = `${row.name}.${row.ext}`;
    const sha = row.github_sha || (await githubSourceSha(env, path));
    if (!sha) return bad("That image isn't in the repo any more; nothing to delete.", 409);
    try {
      await githubDeleteSource(env, path, sha, `Remove ${row.name}`);
    } catch (err) {
      return bad(`Delete failed: ${err.message}`, 502);
    }
  }

  await env.BUCKET.delete(`emotes/${row.id}.${row.ext}`);
  await env.DB.prepare(`DELETE FROM emotes WHERE id = ?`).bind(row.id).run();
  return json({ ok: true });
}
