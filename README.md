# Twitch Emotes: Sanity Capped

A custom emote pack for the TwitchEmotes addon, plus a sound pack for the Soundboard addon. Each adds the guild's own content without modifying the addon it plugs into, so both update independently.

Works on retail and WoW: Forever. Both install the same release, so the emotes and sounds are always identical on the two.

## Installation

1. Install TwitchEmotes for the emotes, Soundboard for the sounds, or both.
2. In WoWUp, go to **Get Addons → Install from URL** and paste:
   ```
   https://github.com/zakpruitt/TwitchEmotes_SanityCapped
   ```
3. Turn on auto-update for the addon so new emotes and sounds arrive on their own.

The emotes and the sounds are independent. Whichever of TwitchEmotes and Soundboard you don't have, that half stays quiet and the other works as normal.

Everyone needs the pack to see the emotes. Anyone without it just sees the text, like `scFuck`.

## Usage

Type an emote name in chat, or pick one from the `:` autocomplete or the emote dropdown.

| Command      | Description                       |
| ------------ | --------------------------------- |
| `/tesc`      | Show how many emotes loaded       |
| `/tesc list` | List every emote with its icon    |

## Sounds

The install adds a second addon folder, `Soundboard_SanityCapped`, which puts the guild's sounds in Soundboard under a **Sanity Capped** category. Play one from the Soundboard menu or type its command, like `/scmlg`. Everyone in your party, raid or guild who has the pack hears it.

| Command          | Description                  |
| ---------------- | ---------------------------- |
| `/scsounds`      | Show how many sounds loaded  |
| `/scsounds list` | List every sound's command   |

Sounds are MP3s up to 20 seconds. Add them through the site, the same as emotes: once approved, the MP3 is committed to `Soundboard_SanityCapped/Sounds/` and a release is built. To test locally, drop an MP3 in that folder and run `python tools/build_sounds.py` (no dependencies).

A sound's name follows the emote rules below, then drops everything but letters and digits, because it becomes a slash command: `vine boom!.mp3` is `scVineboom`, typed `/scvineboom`.

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

The same rules live in `tools/naming.py`, `server/.../Naming.java` and `server/.../naming.js`. Change all three together.

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
Soundboard_SanityCapped/       the sound pack, a second addon in the same zip
  Sounds/                      the MP3s, the source of truth
  SoundData.lua                generated sound table
tools/source/                  original images, the source of truth
tools/build_emotes.py          converts source images into textures
tools/build_sounds.py          lists the MP3s and measures their length
tools/naming.py                naming rules shared by both builds
server/                        the upload and approval site
.github/workflows/release.yml  builds and publishes releases
```

Only the `.toc`, Lua files, `Emotes/` and `Soundboard_SanityCapped/` ship in the release zip.

<!-- portfolio
name: Sanity Capped Emotes
live: none
year: 2026
order: 4
tags: Lua, Java, GitHub Actions
summary: My guild's World of Warcraft emote pack. Members upload emotes through a web app, and GitHub
  Actions builds and publishes each new release.
-->
