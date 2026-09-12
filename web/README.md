# Emote site

A Cloudflare Pages site where the guild uploads emotes. Approving one commits the
image to `tools/source/` in this repo, which makes GitHub Actions rebuild the
addon and publish a release that WoWUp picks up.

```
public/     browse, upload and admin pages (plain HTML + modules, no build step)
functions/  the API, running as Cloudflare Pages Functions
schema.sql  D1 tables
scripts/    seed the existing pack into D1 + R2
```

`public/naming.js` is a port of `sanitize()`/`apply_prefix()` from
`tools/build_emotes.py` and is imported by both the browser and the API, so the
trigger word the site promises is the one the build produces.

## One-time setup

Everything below is free tier.

```bash
cd web
npx wrangler login

# storage
npx wrangler d1 create sanity-capped-emotes        # paste the id into wrangler.toml
npx wrangler r2 bucket create sanity-capped-emotes
npx wrangler d1 execute sanity-capped-emotes --remote --file=schema.sql

# seed the emotes that already shipped
py -3.13 scripts/seed.py
npx wrangler d1 execute sanity-capped-emotes --remote --file=scripts/seed.sql
bash scripts/seed_r2.sh
```

Then create the Pages project (dashboard → Workers & Pages → Create → Pages →
connect this repo):

| Setting                | Value  |
| ---------------------- | ------ |
| Root directory         | `web`  |
| Build command          | *(none)* |
| Build output directory | `public` |
| Build watch paths      | `web/*` — so emote commits don't redeploy the site |

Bind `DB` to the D1 database and `BUCKET` to the R2 bucket under Settings →
Functions → Bindings, set the `GITHUB_REPO` variable, and add three secrets:

| Secret           | What                                                                 |
| ---------------- | -------------------------------------------------------------------- |
| `GUILD_PASSCODE` | What you paste in guild chat. Lets people upload.                     |
| `ADMIN_PASSCODE` | Yours. Approve, reject, remove.                                       |
| `GITHUB_TOKEN`   | Fine-grained PAT, this repo only, **Contents: read and write**.       |

## Local development

```bash
cd web
cp .dev.vars.example .dev.vars     # fill in the three secrets
npx wrangler pages dev . --d1 DB=sanity-capped-emotes --r2 BUCKET=sanity-capped-emotes
npx wrangler d1 execute sanity-capped-emotes --local --file=schema.sql
```

Local D1 and R2 live under `.wrangler/`, so nothing you test touches the real
queue — except approval, which commits to GitHub for real. Point `GITHUB_REPO`
at a throwaway repo if you want to exercise that path.

## How duplicates are caught

1. **Name** — the final trigger word must be free among pending and approved emotes.
2. **Exact file** — SHA-256 of the bytes, checked server-side.
3. **Looks the same** — a 64-bit dHash of the first frame, computed in the browser.
   Within 6 bits of an existing emote it's a warning, flagged in the queue for you,
   not a block: a resized or recoloured variant is sometimes the point.

Rejected rows release their name and hash, so someone can try again.
