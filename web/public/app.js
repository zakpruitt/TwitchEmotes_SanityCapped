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

/**
 * 64-bit difference hash of the image's first frame: downscale to 9x8 greyscale
 * and record whether each pixel is brighter than the one to its right. Near
 * duplicates (a resize, a recompress, a recolour) land within a few bits.
 */
export async function dhash(file) {
  const bitmap = await createImageBitmap(file).catch(() => null);
  if (!bitmap) return null;
  const canvas = document.createElement("canvas");
  canvas.width = 9;
  canvas.height = 8;
  const ctx = canvas.getContext("2d", { willReadFrequently: true });
  ctx.drawImage(bitmap, 0, 0, 9, 8);
  const { data } = ctx.getImageData(0, 0, 9, 8);

  const grey = [];
  for (let i = 0; i < data.length; i += 4) {
    const a = data[i + 3] / 255;
    // Composite onto black so transparent padding doesn't read as bright noise.
    grey.push((0.299 * data[i] + 0.587 * data[i + 1] + 0.114 * data[i + 2]) * a);
  }

  let bits = "";
  for (let y = 0; y < 8; y++) {
    for (let x = 0; x < 8; x++) {
      bits += grey[y * 9 + x] > grey[y * 9 + x + 1] ? "1" : "0";
    }
  }
  return bits.match(/.{4}/g).map((n) => parseInt(n, 2).toString(16)).join("");
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
