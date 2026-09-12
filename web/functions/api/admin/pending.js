import { json, bad, isAdmin, rowToEmote } from "../../_shared.js";

export async function onRequestGet({ request, env }) {
  if (!isAdmin(request, env)) return bad("Admin passcode required.", 401);
  const { results } = await env.DB.prepare(
    `SELECT * FROM emotes WHERE status = 'pending' ORDER BY created_at ASC`
  ).all();
  return json({ pending: results.map(rowToEmote) });
}
