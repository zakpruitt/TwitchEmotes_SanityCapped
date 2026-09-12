// Talking to the server, and the two small things every page needs.

/** Every endpoint answers with JSON, and every failure with {error}. */
export async function api(path, { passcode, ...init } = {}) {
  const response = await fetch(path, {
    ...init,
    headers: { ...(init.headers ?? {}), ...(passcode ? { "X-Passcode": passcode } : {}) },
  });
  const body = response.status === 204 ? {} : await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(body.error ?? `Request failed (${response.status})`);
  }
  return body;
}

export async function postJson(path, body, passcode) {
  return api(path, {
    method: "POST",
    passcode,
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
}

/** Passcodes are remembered per browser so nobody retypes them every visit. */
export const remembered = {
  get(key) {
    return localStorage.getItem(key) ?? "";
  },
  set(key, value) {
    localStorage.setItem(key, value);
  },
};

/** The server hashes uploads itself; this only pre-empts an obvious re-upload. */
export async function sha256(file) {
  const digest = await crypto.subtle.digest("SHA-256", await file.arrayBuffer());
  return [...new Uint8Array(digest)].map((b) => b.toString(16).padStart(2, "0")).join("");
}

export function show(element, text, kind = "ok") {
  element.className = `msg ${kind}`;
  element.textContent = text;
}

export function clear(element) {
  element.className = "msg";
  element.textContent = "";
}

export const $ = (selector, root = document) => root.querySelector(selector);
