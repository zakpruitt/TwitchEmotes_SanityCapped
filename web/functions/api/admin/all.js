import { json, bad, isAdmin, rowToEmote } from "../../_shared.js";

export async function onRequestGet({ request, env }) {
  if (!isAdmin(request, env)) return bad("Admin passcode required.", 401);
  const { results } = await env.DB.prepare(
    `SELECT * FROM emotes ORDER BY created_at DESC LIMIT 500`
  ).all();
  return json({ emotes: results.map(rowToEmote) });
}
