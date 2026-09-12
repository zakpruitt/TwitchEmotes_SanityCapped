# Twitch Emotes — Sanity Capped

A companion addon for **TwitchEmotes**. It adds your own emotes without touching
a single file in the TwitchEmotes folder, so the base addon can update freely and
your pack survives.

Your emotes behave exactly like the built-in ones: they render in chat, show up
in the `:` autocomplete, get their own section in the emote dropdown, animate,
and count toward the emote statistics screen.

---

## Adding an emote

1. Drop a `.gif`, `.png`, or `.webp` into `tools/source/`.
2. Name the file after the emote — `copiumChad.gif` becomes `scCopiumChad`
   in chat. See [Naming](#naming) for how the name is derived.
3. Run the build script:

   ```
   py -3.13 tools/build_emotes.py
   ```

4. `/reload` in game (or log out and back in the first time).

That's it. The script resizes, converts to WoW's texture format, flattens
animations into sprite sheets, and regenerates `EmoteData.lua`.

Deleting an image from `tools/source/` and re-running removes the emote —
`tools/source/` is the source of truth, and stale textures get cleaned up.

**First-time setup:** the script needs Pillow.

```
py -3.13 -m pip install pillow
```

---

## Naming

TwitchEmotes splits chat messages on whitespace and on `, ' < > ? - . !`, so a
trigger word containing any of those characters can **never** match in chat.
This is a silent failure mode: the emote still appears in the `:` autocomplete
list (that only needs a table key) but posting it just shows the plain text.

The build script fixes this for you rather than letting it happen:

- A trailing size suffix is dropped — `peepoHmm-128.png` becomes `peepoHmm`,
  `catJAM_2x.gif` becomes `catJAM`. Emote sites like 7TV and BTTV add these to
  downloads, so most files you grab will need it.
- Any remaining break characters are removed — `pepe-laugh.png` becomes
  `pepelaugh`.
- Everything is then prefixed with `sc` in camelCase, so the pack has its own
  namespace and can't collide with the ~4000 emotes in the base addon.

| File                  | You type        |
| --------------------- | --------------- |
| `cloudzUlt-128.png`   | `scCloudzUlt`   |
| `FUCK-128.png`        | `scFuck`        |
| `VynChatting-128.png` | `scVynChatting` |
| `catJAM_2x.gif`       | `scCatJAM`      |

Names that are entirely uppercase get title-cased first, because `scFUCK` reads
worse than `scFuck`. A file already starting with `scSomething` is left alone, so
re-running the build never produces `scScSomething`.

Every rename is printed under **"Renamed so chat can match them"** when you
build. That right-hand column is what you type in game.

To change or drop the prefix, edit `PREFIX` at the top of `tools/build_emotes.py`
(set it to `""` to disable). To force one specific emote's name, use the `name`
field in `tools/emotes.json` — see below.

Names are case-sensitive, and `_`, `+` and `:` are all safe to use. Colons work
if you wrap both ends — `:sadge:` is fine, `sad:ge` is not.

The script also warns when a name would shadow an emote from the base pack, and
when two source files would collapse to the same trigger word (the second one is
skipped rather than silently overwriting the first).

---

## Tuning individual emotes (optional)

Create `tools/emotes.json` to override the defaults for specific emotes. Keys
are filename stems; every field is optional.

```json
{
  "OOOO-128": {
    "name": "scOOOO",
    "framerate": 20,
    "pingpong": true
  },
  "cloudzUlt-128": {
    "aliases": ["scUlt", "scCz"]
  },
  "PewW-128": {
    "size": "LARGE"
  }
}
```

Keys can be the source filename (without extension), the cleaned name, or the
final prefixed name. `framerate` and `pingpong` only affect animated emotes.

| Field       | Meaning                                                              |
| ----------- | -------------------------------------------------------------------- |
| `name`      | Force an exact trigger word, bypassing the prefix and camelCase rules. |
| `aliases`   | Extra words that post the same emote. Used verbatim — add `sc` yourself if you want it. |
| `framerate` | Frames per second. Overrides the GIF's own timing.                    |
| `size`      | `28:28` (default), or `LARGE` / `XLARGE` / `XXLARGE` — these scale up only when the user has "large emotes" enabled in TwitchEmotes' options. |
| `pingpong`  | Play the animation forwards then backwards instead of looping.        |

---

## Limits worth knowing

These come from WoW's texture handling, not from this addon:

- **64 frames max** per animated emote. Textures are capped at 2048px tall and
  each frame is 32px. Longer GIFs are evenly sampled down to 64 frames, and the
  framerate is recalculated so the loop still takes the same wall time.
- **32×32 px per frame.** Emotes render at 28×28 in chat, so there's no gain
  from more. Non-square images are letterboxed onto a transparent square rather
  than stretched.
- **~30 fps ceiling.** The animator ticks at roughly 30fps; anything faster just
  drops frames.
- Big animated emotes are real memory. A 64-frame emote is a 256KB texture. A
  few dozen is fine; a few hundred will be felt at load.

---

## How the conversion works

Worth knowing if you ever need to debug a texture that won't load.

Each emote is written as a **32-bit BGRA TGA, RLE-compressed, bottom-left
origin** (image type 10, descriptor `0x08`) — byte-for-byte the same format the
base TwitchEmotes pack ships, which is the only format its animator understands.

Animated emotes become a **vertical sprite sheet**: 32px wide, one frame per
32px row, frame 0 at the top, total height padded to a power of two. The addon
plays them by rewriting the texture's V coordinates each tick, which is why the
frame count and image height have to be exact — those go into `ns.animations` in
`EmoteData.lua`.

Framerate is derived from the GIF's own per-frame delays. A delay of 0 means "as
fast as possible", which every real player clamps to ~100ms, so the script does
the same rather than producing an absurd framerate.

---

## Installing and sharing the pack

Everyone installs it through **WoWUp -> Get Addons -> Install from URL** with

```
https://github.com/zakpruitt/TwitchEmotes_SanityCapped
```

WoWUp reads the latest GitHub Release, which CI publishes automatically, and
offers an update whenever a new emote lands. They need TwitchEmotes itself, which
the `.toc` declares as a dependency, so WoW quietly refuses to load this without
it rather than throwing errors.

Nothing in the addon is tied to one machine. Texture paths are WoW virtual paths
(`Interface\AddOns\...`), not filesystem paths, and the root is derived from the
addon's own folder name at load time.

Everyone needs the same pack installed to see the same emotes. Emotes are
rendered locally from a texture on disk; what actually crosses the wire is the
plain text `scCopiumChad`. Someone without the pack just sees that word.

---

## Adding emotes without the build step

The guild uploads at the emote site (`web/`, on Cloudflare Pages). An upload sits
in a queue until it's approved; approving commits the image into `tools/source/`
here, and `.github/workflows/release.yml` rebuilds the textures, bumps the
version, and publishes a release. Nobody needs Python or a zip file.

The local flow above still works and is the fastest way to test something before
it ships. `tools/source/` stays the source of truth either way.

---

## In-game commands

| Command      | Does                                     |
| ------------ | ---------------------------------------- |
| `/tesc`      | How many custom emotes loaded.           |
| `/tesc list` | Print every custom emote with its icon.  |

`/sanityemotes` works as a longer alias.

---

## How it hooks in

`Core.lua` waits for `PLAYER_LOGIN` (after TwitchEmotes has finished its own
setup) and then:

- merges into `TwitchEmotes_defaultpack`, `TwitchEmotes_emoticons`, and
  `TwitchEmotes_animation_metadata`
- appends to `AllTwitchEmoteNames` in place — TwitchEmotes builds that list once
  during its own load, and the autocomplete library holds it by reference
- appends a dropdown category, respecting the saved favourite setting instead of
  re-checking it every login
- wraps `TwitchEmotesAnimator_UpdateEmoteInFontString`

That last one is the only real workaround. The base animator finds animated
emotes with a pattern hardcoded to `Interface\AddOns\TwitchEmotes\Emotes`, so
emotes served from this folder would sit frozen on frame 0. The wrapper calls
the original first (base-pack emotes keep working exactly as before) and then
runs the same pass over this addon's path.

**Known cosmetic gap:** the single large "top emote" icon at the top of the
statistics screen uses that same hardcoded pattern in a spot that can't be
wrapped. If one of your custom emotes becomes your most-used, that one icon
shows a still frame. The per-row emotes on the same screen animate fine.
