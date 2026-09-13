# Twitch Emotes: Sanity Capped

A custom emote pack for the TwitchEmotes addon. It adds the guild's emotes without modifying TwitchEmotes, so both update independently.

## Installation

1. Install TwitchEmotes.
2. In WoWUp, go to **Get Addons → Install from URL** and paste:
   ```
   https://github.com/zakpruitt/TwitchEmotes_SanityCapped
   ```
3. Turn on auto-update for the addon so new emotes arrive on their own.

Everyone needs the pack to see the emotes. Anyone without it just sees the text, like `scFuck`.

## Usage

Type an emote name in chat, or pick one from the `:` autocomplete or the emote dropdown.

| Command      | Description                       |
| ------------ | --------------------------------- |
| `/tesc`      | Show how many emotes loaded       |
| `/tesc list` | List every emote with its icon    |

## Adding Emotes

**Through the site (normal way):** upload at the emote site. Once approved, the image is committed to `tools/source/` and GitHub Actions builds and publishes a new release.

**Locally (for testing):**

```bash
pip install pillow
python tools/build_emotes.py
```

Drop a `.gif`, `.png`, `.webp` or `.jpg` into `tools/source/`, run the script, then `/reload` in game. Delete an image and rebuild to remove it.

## Naming

The file name becomes the chat trigger word:

| File                | Trigger word    |
| ------------------- | --------------- |
| `cloudzUlt-128.png` | `scCloudzUlt`   |
| `FUCK-128.png`      | `scFuck`        |
| `catJAM_2x.gif`     | `scCatJAM`      |
| `pepe-laugh.png`    | `scPepelaugh`   |

- Size suffixes like `-128` or `_2x` are dropped.
- Characters chat splits on (spaces and `, ' < > ? - . !`) are removed.
- Everything gets an `sc` prefix. All-caps names are title-cased.

The same rules live in `tools/build_emotes.py`, `server/.../Naming.java` and `server/.../naming.js`. Change all three together.

## Per-Emote Options

Copy `tools/emotes.json.example` to `tools/emotes.json`. Keys are file names without the extension.

| Field       | Description                                        |
| ----------- | -------------------------------------------------- |
| `name`      | Force an exact trigger word                        |
| `aliases`   | Extra words that post the same emote               |
| `framerate` | Override the GIF's own timing                      |
| `size`      | `28:28` (default), `LARGE`, `XLARGE` or `XXLARGE`  |
| `pingpong`  | Play forwards then backwards                       |

## Limits

- 32×32 per frame, shown at 28×28
- 64 frames max per animation (longer GIFs are sampled down)
- About 30 fps max

## Project Structure

```
Core.lua                       hooks the pack into TwitchEmotes
EmoteData.lua                  generated emote table
Emotes/                        generated .tga textures (what WoW loads)
TwitchEmotes_SanityCapped.toc
tools/source/                  original images, the source of truth
tools/build_emotes.py          converts source images into textures
server/                        the upload and approval site
.github/workflows/release.yml  builds and publishes releases
```

Only the `.toc`, Lua files and `Emotes/` ship in the release zip.
