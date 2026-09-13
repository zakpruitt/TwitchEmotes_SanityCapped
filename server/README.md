# Sanity Capped Emote Site

Spring Boot app where the guild uploads emotes and an admin approves them. Approving commits the image to `tools/source/`, which triggers a new addon release.

## Tech Stack

Java 21, Spring Boot 4, Spring Data JPA on SQLite, Thymeleaf. Images are stored on a local volume.

## Running Locally

```bash
cd server
DATA_DIR=./data ADMIN_PASSCODE=admin-test mvn spring-boot:run
```

Open http://localhost:8080. On startup it imports every emote already in `tools/source/`. Without `GITHUB_TOKEN`, uploads and rejections work but approvals are disabled.

## Testing

```bash
mvn test
```

## Configuration

| Variable         | Description                                            |
| ---------------- | ------------------------------------------------------ |
| `DATA_DIR`       | Where images and `emotes.db` live (persistent volume)  |
| `ADMIN_PASSCODE` | Allows approve, reject, remove and resync              |
| `GITHUB_TOKEN`   | Fine-grained PAT for this repo, Contents read/write    |
| `GITHUB_REPO`    | `owner/name`, defaults to this repo                    |
| `PORT`           | Defaults to `8080`                                     |

Health check: `/actuator/health`

## Deployment (Fly.io)

```bash
cd server
fly launch --no-deploy
fly volumes create emotes_data --size 1
fly secrets set ADMIN_PASSCODE=... GITHUB_TOKEN=...
fly deploy
```

The volume is a cache. Approved emotes live in the repo, so a fresh volume repopulates on boot.

## Duplicate Checks

1. **Name:** the trigger word must not already be pending or approved.
2. **Exact file:** SHA-256 match is refused.
3. **Similar image:** a perceptual hash within 6 bits shows a warning in the queue but is allowed.

Rejecting an emote frees its name and hash.

## Project Structure

```
src/main/java/com/zakpruitt/sanitycapped/
  config/      app properties, clock, converters
  emote/
    model/       Emote, UploadLog, EmoteStatus
    dto/         internal records
    repository/  Spring Data repositories
    service/     upload, review, duplicates, publishing, repo sync
    exception/   EmoteException
  image/       storage, decoding, hashing
  naming/      trigger word rules
  github/      GitHub API client
  web/
    api/         JSON endpoints
    view/        page controller
    dto/         request and response records
    security/    passcode checks
    exception/   error handling
src/main/resources/
  templates/   Thymeleaf pages
  static/      CSS and JS
  schema.sql   database schema
```
