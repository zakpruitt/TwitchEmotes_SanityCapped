import { json } from "../_shared.js";

/** Latest release, so the site can show what WoWUp will pull. */
export async function onRequestGet({ env }) {
  const repo = env.GITHUB_REPO;
  const out = { repo, repoUrl: `https://github.com/${repo}`, release: null };
  try {
    const res = await fetch(`https://api.github.com/repos/${repo}/releases/latest`, {
      headers: { accept: "application/vnd.github+json", "user-agent": "sanity-capped-emotes" },
      cf: { cacheTtl: 120, cacheEverything: true },
    });
    if (res.ok) {
      const r = await res.json();
      out.release = { tag: r.tag_name, url: r.html_url, publishedAt: r.published_at };
    }
  } catch { /* the site still works without it */ }
  return json(out);
}
