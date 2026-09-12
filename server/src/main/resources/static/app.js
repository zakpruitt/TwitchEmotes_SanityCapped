// Small shared helpers for the three pages.

export const store = {
  get pass() { return localStorage.getItem("pass") || ""; },
  set pass(v) { localStorage.setItem("pass", v); },
  get admin() { return localStorage.getItem("adminPass") || ""; },
  set admin(v) { localStorage.setItem("adminPass", v); },
};

export async function api(path, { pass, ...init } = {}) {
  const res = await fetch(path, {
    ...init,
    headers: { ...(init.headers || {}), ...(pass ? { "x-passcode": pass } : {}) },
  });
  const body = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(body.error || `Request failed (${res.status})`);
  return body;
}

export async function sha256Hex(file) {
  const digest = await crypto.subtle.digest("SHA-256", await file.arrayBuffer());
  return [...new Uint8Array(digest)].map((b) => b.toString(16).padStart(2, "0")).join("");
}

export function timeAgo(seconds) {
  const d = Math.floor(Date.now() / 1000) - seconds;
  if (d < 60) return "just now";
  if (d < 3600) return `${Math.floor(d / 60)}m ago`;
  if (d < 86400) return `${Math.floor(d / 3600)}h ago`;
  return `${Math.floor(d / 86400)}d ago`;
}

export function toast(el, text, kind = "ok") {
  el.className = `msg ${kind}`;
  el.textContent = text;
}

export function header(active) {
  return `
    <header class="site">
      <div class="brand">Sanity Capped <b>Emotes</b></div>
      <nav>
        <a href="/" class="${active === "browse" ? "active" : ""}">Browse</a>
        <a href="/upload.html" class="${active === "upload" ? "active" : ""}">Upload</a>
        <a href="/admin.html" class="${active === "admin" ? "active" : ""}">Admin</a>
      </nav>
    </header>`;
}
