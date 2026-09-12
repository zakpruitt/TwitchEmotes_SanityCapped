import { json, isGuild, isAdmin } from "../_shared.js";

/** Lets the pages tell "wrong passcode" from "not an admin" without guessing. */
export async function onRequestGet({ request, env }) {
  return json({ guild: isGuild(request, env), admin: isAdmin(request, env) });
}
