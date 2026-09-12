import { json, triggerWord, hamming, SIMILAR_THRESHOLD } from "../_shared.js";

/** Live feedback while someone is filling in the upload form. */
export async function onRequestGet({ request, env }) {
  const url = new URL(request.url);
  const name = triggerWord(url.searchParams.get("name") || "");
  const sha = (url.searchParams.get("sha") || "").toLowerCase();
  const dhash = (url.searchParams.get("dhash") || "").toLowerCase();

  const out = { name, nameTaken: null, exact: null, similar: null };

  if (name) {
    const row = await env.DB.prepare(
      `SELECT name, status FROM emotes WHERE name = ? AND status IN ('pending','approved')`
    ).bind(name).first();
    if (row) out.nameTaken = row.status;
  }

  if (sha) {
    const row = await env.DB.prepare(
      `SELECT name FROM emotes WHERE sha256 = ? AND status IN ('pending','approved')`
    ).bind(sha).first();
    if (row) out.exact = row.name;
  }

  if (dhash && !out.exact) {
    const { results } = await env.DB.prepare(
      `SELECT name, dhash FROM emotes WHERE dhash IS NOT NULL AND status IN ('pending','approved')`
    ).all();
    const near = results.find((r) => hamming(dhash, r.dhash) <= SIMILAR_THRESHOLD);
    if (near) out.similar = near.name;
  }

  return json(out);
}
