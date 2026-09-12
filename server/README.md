# Emote site

Spring Boot 4 app where the guild uploads emotes and you approve them. Approving
commits the image into `tools/source/` in this repo, which makes GitHub Actions
rebuild the addon and publish a release WoWUp picks up. Nobody pushes anything by
hand.

```
src/main/java/gg/sanitycapped/emotes/
  core/    naming, image inspection, hashes, EmoteService (the rules live here)
  store/   SQLite via JdbcClient, plus images on local disk
  github/  the only outbound calls: commit, delete, list, latest release
  web/     controllers, passcode checks, error shape
src/main/resources/static/   browse, upload and admin pages (no build step)
```

Java 21, SQLite, images on a local volume. No database server and nothing to
provision.

## Running it locally

```bash
cd server
DATA_DIR=./data GUILD_PASSCODE=letmein ADMIN_PASSCODE=admin-test mvn spring-boot:run
```

Open http://localhost:8080. On first boot it reads `tools/source/` from the repo
over the GitHub API and adopts everything already in the pack, so browse is
populated immediately — no seeding step. Without `GITHUB_TOKEN` set, uploads and
rejections work but approving returns "No GitHub token configured", since
approval means committing.

```bash
mvn test     # naming parity with build_emotes.py, plus the upload/approve flow
```

## Configuration

All of it is environment variables (see `src/main/resources/application.yml`):

| Variable         | What                                                                |
| ---------------- | ------------------------------------------------------------------- |
| `DATA_DIR`       | Where images and `emotes.db` live. Must be a persistent volume.      |
| `GUILD_PASSCODE` | What you paste in guild chat. Lets people upload.                    |
| `ADMIN_PASSCODE` | Yours. Approve, reject, remove, resync.                              |
| `GITHUB_TOKEN`   | Fine-grained PAT, this repo only, **Contents: read and write**.      |
| `GITHUB_REPO`    | `owner/name`. Defaults to this repo.                                 |
| `PORT`           | Defaults to 8080.                                                    |

## Deploying to Fly.io

```bash
cd server
fly launch --no-deploy          # keeps fly.toml; pick your own app name
fly volumes create emotes_data --size 1
fly secrets set GUILD_PASSCODE=... ADMIN_PASSCODE=... GITHUB_TOKEN=...
fly deploy
```

`fly.toml` mounts the volume at `/data` and lets the machine sleep when idle, so
this sits inside the free allowance. The first request after a nap waits a few
seconds for the JVM to wake.

The volume holds the pending queue and a cache of the images. It is not the only
copy of the pack: approved emotes live in `tools/source/` in the repo, and a
fresh volume repopulates itself from there on boot.

## How duplicates are caught

1. **Name** — the final trigger word must be free among pending and approved emotes.
2. **Exact file** — SHA-256 of the bytes. The browser also pre-checks this while you fill in the form.
3. **Looks the same** — a 64-bit dHash of the first frame, computed server-side after
   actually decoding the image. Within 6 bits of an existing emote it's a warning
   flagged on the queue, not a block: a resized or recoloured variant is sometimes
   the point.

Rejected rows release their name and hash, so someone can try again.

`static/naming.js`, `core/Naming.java` and `sanitize()`/`apply_prefix()` in
`tools/build_emotes.py` are three ports of the same rules. `NamingTest` pins the
Java one against the Python one; if you change the rules, change all three.
