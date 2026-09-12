import { triggerWord } from "../public/naming.js";

export { triggerWord };

export const EXT_BY_TYPE = {
  "image/gif": "gif",
  "image/png": "png",
  "image/webp": "webp",
  "image/jpeg": "jpg",
};

export const MAX_BYTES = 2 * 1024 * 1024;

export function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" },
  });
}

export function bad(message, status = 400) {
  return json({ error: message }, status);
}

/** Length-safe-ish comparison; these are guild passcodes, not bank creds. */
function matches(given, expected) {
  if (!expected || !given || given.length !== expected.length) return false;
  let diff = 0;
  for (let i = 0; i < given.length; i++) diff |= given.charCodeAt(i) ^ expected.charCodeAt(i);
  return diff === 0;
}

export function isGuild(request, env) {
  const p = request.headers.get("x-passcode") || "";
  return matches(p, env.GUILD_PASSCODE) || matches(p, env.ADMIN_PASSCODE);
}

export function isAdmin(request, env) {
  return matches(request.headers.get("x-passcode") || "", env.ADMIN_PASSCODE);
}

export async function sha256Hex(buffer) {
  const digest = await crypto.subtle.digest("SHA-256", buffer);
  return [...new Uint8Array(digest)].map((b) => b.toString(16).padStart(2, "0")).join("");
}

/** Sniff the real type; a renamed .exe should not become an emote. */
export function sniffType(bytes) {
  const b = new Uint8Array(bytes);
  const starts = (...sig) => sig.every((v, i) => b[i] === v);
  if (starts(0x47, 0x49, 0x46, 0x38)) return "image/gif";
  if (starts(0x89, 0x50, 0x4e, 0x47)) return "image/png";
  if (starts(0xff, 0xd8, 0xff)) return "image/jpeg";
  if (starts(0x52, 0x49, 0x46, 0x46) && b[8] === 0x57 && b[9] === 0x45 && b[10] === 0x42 && b[11] === 0x50)
    return "image/webp";
  return null;
}

export function hamming(a, b) {
  if (!a || !b || a.length !== b.length) return 64;
  let d = 0;
  for (let i = 0; i < a.length; i++) {
    let x = parseInt(a[i], 16) ^ parseInt(b[i], 16);
    while (x) { d += x & 1; x >>= 1; }
  }
  return d;
}

export const SIMILAR_THRESHOLD = 6;

export function toBase64(arrayBuffer) {
  const bytes = new Uint8Array(arrayBuffer);
  let binary = "";
  const CHUNK = 0x8000;
  for (let i = 0; i < bytes.length; i += CHUNK) {
    binary += String.fromCharCode.apply(null, bytes.subarray(i, i + CHUNK));
  }
  return btoa(binary);
}

function ghHeaders(env) {
  return {
    authorization: `Bearer ${env.GITHUB_TOKEN}`,
    accept: "application/vnd.github+json",
    "user-agent": "sanity-capped-emotes",
    "content-type": "application/json",
  };
}

/** Commit an image into tools/source/. This is what triggers a release build. */
export async function githubPutSource(env, path, arrayBuffer, message) {
  const url = `https://api.github.com/repos/${env.GITHUB_REPO}/contents/tools/source/${path}`;
  const res = await fetch(url, {
    method: "PUT",
    headers: ghHeaders(env),
    body: JSON.stringify({ message, content: toBase64(arrayBuffer) }),
  });
  const body = await res.json();
  if (!res.ok) throw new Error(`GitHub ${res.status}: ${body.message || "commit failed"}`);
  return body.content.sha;
}

/** The seeded emotes predate the site, so their blob sha has to be looked up. */
export async function githubSourceSha(env, path) {
  const url = `https://api.github.com/repos/${env.GITHUB_REPO}/contents/tools/source/${path}`;
  const res = await fetch(url, { headers: ghHeaders(env) });
  if (!res.ok) return null;
  const body = await res.json();
  return body.sha || null;
}

export async function githubDeleteSource(env, path, sha, message) {
  const url = `https://api.github.com/repos/${env.GITHUB_REPO}/contents/tools/source/${path}`;
  const res = await fetch(url, {
    method: "DELETE",
    headers: ghHeaders(env),
    body: JSON.stringify({ message, sha }),
  });
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(`GitHub ${res.status}: ${body.message || "delete failed"}`);
  }
}

export function nowSeconds() {
  return Math.floor(Date.now() / 1000);
}

export function rowToEmote(r) {
  return {
    id: r.id,
    name: r.name,
    url: `/img/${r.id}.${r.ext}`,
    uploader: r.uploader,
    status: r.status,
    animated: !!r.animated,
    note: r.note || null,
    createdAt: r.created_at,
  };
}
