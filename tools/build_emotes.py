#!/usr/bin/env python3
"""Convert tools/source/*.{gif,png,webp} into WoW emote textures + EmoteData.lua.

See README.md for naming rules, limits and the emotes.json options.
Requires Pillow:  py -3.13 -m pip install pillow
"""

import json
import re
import struct
import sys
from pathlib import Path

try:
    from PIL import Image, ImageSequence
except ImportError:
    sys.exit("Pillow is not installed. Run:  py -3.13 -m pip install pillow")

FRAME = 32
MAX_FRAMES = 2048 // FRAME
MAX_FPS = 30
DEFAULT_FPS = 15
DISPLAY = "28:28"
PREFIX = "sc"

SOURCE_EXT = {".gif", ".png", ".webp", ".apng", ".jpg", ".jpeg", ".bmp"}

HERE = Path(__file__).resolve().parent
ADDON_DIR = HERE.parent
SOURCE_DIR = HERE / "source"
OUT_DIR = ADDON_DIR / "Emotes"
LUA_OUT = ADDON_DIR / "EmoteData.lua"
OVERRIDES = HERE / "emotes.json"
MANIFEST = HERE / "manifest.json"
BASE_EMOTES_LUA = ADDON_DIR.parent / "TwitchEmotes" / "Emotes.lua"


# --- textures ---------------------------------------------------------------

def write_tga(img, path):
    """32-bit BGRA, RLE, bottom-left origin: the format the base pack ships."""
    img = img.convert("RGBA")
    w, h = img.size
    px = img.load()

    body = bytearray()
    for y in reversed(range(h)):  # bottom-left origin stores rows bottom-up
        row = bytearray()
        for x in range(w):
            r, g, b, a = px[x, y]
            row += bytes((b, g, r, a))
        body += _rle_scanline(bytes(row), w)

    header = struct.pack(
        "<BBBHHBHHHHBB",
        0, 0, 10,      # no id, no colour map, RLE true-colour
        0, 0, 0,
        0, 0, w, h,
        32, 0x08,      # 32bpp, 8 alpha bits, bottom-left origin
    )
    path.write_bytes(header + bytes(body))


def _rle_scanline(row, width):
    """RLE one scanline. Packets never span scanlines, per the TGA spec."""
    def at(i):
        return row[i * 4:i * 4 + 4]

    out = bytearray()
    i = 0
    while i < width:
        run = 1
        while run < 128 and i + run < width and at(i + run) == at(i):
            run += 1

        if run > 1:
            out.append(0x80 | (run - 1))
            out += at(i)
            i += run
            continue

        start = i
        while i - start < 128 and i < width:
            if i + 1 < width and at(i + 1) == at(i):
                break
            i += 1
        out.append(i - start - 1)
        out += row[start * 4:i * 4]
    return bytes(out)


# --- images -----------------------------------------------------------------

def fit(frame):
    """Scale into FRAME x FRAME, keeping aspect, centred on transparent."""
    frame = frame.convert("RGBA")
    w, h = frame.size
    if not w or not h:
        raise ValueError("zero-sized frame")
    scale = min(FRAME / w, FRAME / h)
    nw, nh = max(1, round(w * scale)), max(1, round(h * scale))
    canvas = Image.new("RGBA", (FRAME, FRAME), (0, 0, 0, 0))
    canvas.paste(frame.resize((nw, nh), Image.LANCZOS),
                 ((FRAME - nw) // 2, (FRAME - nh) // 2))
    return canvas


def load_frames(src):
    """Return (frames, loop_duration_seconds); duration is 0 for stills."""
    with Image.open(src) as im:
        if getattr(im, "n_frames", 1) <= 1:
            return [fit(im)], 0.0

        frames, total_ms = [], 0
        for raw in ImageSequence.Iterator(im):
            frames.append(fit(raw))
            delay = raw.info.get("duration", 0) or 0
            total_ms += delay if delay >= 20 else 100  # 0 means "as fast as possible"
        return frames, total_ms / 1000.0


def decimate(frames):
    """Sample evenly down to MAX_FRAMES, leaving the loop duration unchanged."""
    if len(frames) <= MAX_FRAMES:
        return frames
    step = len(frames) / MAX_FRAMES
    return [frames[min(len(frames) - 1, int(i * step))] for i in range(MAX_FRAMES)]


def sheet(frames):
    """Stack frames vertically, frame 0 at the top, padded to a power of two."""
    height = 1
    while height < len(frames) * FRAME:
        height <<= 1
    canvas = Image.new("RGBA", (FRAME, height), (0, 0, 0, 0))
    for i, f in enumerate(frames):
        canvas.paste(f, (0, i * FRAME))
    return canvas, height


# --- naming -----------------------------------------------------------------

# Emoticons_InsertEmoticons splits chat on these, so a name containing one can
# never match in chat (it still shows in autocomplete, which is the confusing bit).
CHAT_DELIMITERS = re.compile(r"[\s,'<>?\-.!]+")
SIZE_SUFFIX = re.compile(r"[-_](?:\d{1,4}px|\d{1,4}x|\d{1,4})$", re.I)


def sanitize(stem):
    name = stem
    while SIZE_SUFFIX.search(name):
        name = SIZE_SUFFIX.sub("", name)
    return CHAT_DELIMITERS.sub("", name)


def apply_prefix(name):
    """cloudzUlt -> scCloudzUlt, FUCK -> scFuck, scFoo -> scFoo."""
    if not PREFIX or not name:
        return name
    already = (name.startswith(PREFIX) and len(name) > len(PREFIX)
               and name[len(PREFIX)].isupper())
    if already:
        return name
    core = name.capitalize() if name.isupper() and len(name) > 1 else name[:1].upper() + name[1:]
    return PREFIX + core


def base_trigger_words():
    if not BASE_EMOTES_LUA.exists():
        return set()
    text = BASE_EMOTES_LUA.read_text(encoding="utf-8", errors="replace")
    return set(re.findall(r'^\t\["([^"]+)"\]', text, re.M))


# --- lua output -------------------------------------------------------------

def lua_key(s):
    return s.replace("\\", "\\\\").replace('"', '\\"')


def write_lua(entries):
    out = [
        "-- GENERATED BY tools/build_emotes.py -- DO NOT EDIT BY HAND.",
        "-- Add or remove images in tools/source/ and re-run the script.",
        "",
        "local ADDON, ns = ...",
        "",
        "-- Resolved from the folder name at load time, so renaming the addon",
        "-- folder (and its .toc to match) does not need a rebuild.",
        'ns.EMOTE_ROOT = "Interface\\\\AddOns\\\\" .. ADDON .. "\\\\Emotes\\\\"',
        "",
        "ns.pack = {",
    ]
    for e in entries:
        out.append(f'\t["{lua_key(e["name"])}"] = ns.EMOTE_ROOT '
                   f'.. "{lua_key(e["file"])}:{e["size"]}",')

    out += ["}", "", "ns.emoticons = {"]
    for e in entries:
        for word in [e["name"]] + e["aliases"]:
            out.append(f'\t["{lua_key(word)}"] = "{lua_key(e["name"])}",')

    out += ["}", "", "ns.animations = {"]
    for e in entries:
        a = e["anim"]
        if a:
            out.append(
                f'\t[ns.EMOTE_ROOT .. "{lua_key(e["file"])}"] = '
                f'{{["nFrames"] = {a["nFrames"]}, ["frameWidth"] = {FRAME}, '
                f'["frameHeight"] = {FRAME}, ["imageWidth"] = {FRAME}, '
                f'["imageHeight"] = {a["imageHeight"]}, '
                f'["framerate"] = {a["framerate"]}'
                + (', ["pingpong"] = true' if a["pingpong"] else "") + "},")

    out += ["}", "", "ns.ordered = {"]
    out += [f'\t"{lua_key(e["name"])}",' for e in entries]
    out += ["}", ""]

    LUA_OUT.write_text("\n".join(out), encoding="utf-8")


# --- build ------------------------------------------------------------------

def resolve_name(src, overrides):
    """(trigger_word, config) for a source file, or (None, reason)."""
    raw = src.stem
    bare = sanitize(raw)
    name = apply_prefix(bare)
    cfg = overrides.get(raw) or overrides.get(bare) or overrides.get(name) or {}
    if cfg.get("name"):
        name = sanitize(cfg["name"])
    if not name:
        return None, "no usable trigger word is left once the characters chat breaks on are removed"
    return name, cfg


def build_one(src, name, cfg):
    """Write the .tga and return its EmoteData entry."""
    frames, duration = load_frames(src)
    original = len(frames)
    frames = decimate(frames)
    tga = OUT_DIR / f"{name}.tga"

    if len(frames) == 1:
        write_tga(frames[0], tga)
        anim = None
    else:
        canvas, height = sheet(frames)
        write_tga(canvas, tga)
        fps = cfg.get("framerate")
        if fps is None:
            fps = len(frames) / duration if duration else DEFAULT_FPS
            fps = max(1, min(MAX_FPS, round(fps)))
        anim = {"nFrames": len(frames), "imageHeight": height,
                "framerate": fps, "pingpong": bool(cfg.get("pingpong"))}

    entry = {"name": name, "file": tga.name, "anim": anim, "source": src.name,
             "size": cfg.get("size", DISPLAY), "aliases": cfg.get("aliases", [])}

    kb = tga.stat().st_size / 1024
    if anim:
        note = f"{original} frames"
        if original != len(frames):
            note += f" -> {len(frames)} (capped)"
        print(f"  {name:<24} {note}, {anim['framerate']}fps, {kb:.0f} KB")
    else:
        print(f"  {name:<24} static, {kb:.0f} KB")
    return entry


def write_manifest(entries):
    """Machine-readable index of the pack, for CI release notes and the website."""
    MANIFEST.write_text(json.dumps({
        "count": len(entries),
        "emotes": [{"name": e["name"], "file": e["file"], "source": e["source"],
                    "animated": bool(e["anim"]), "aliases": e["aliases"]}
                   for e in entries],
    }, indent=2) + "\n", encoding="utf-8")


def prune(keep):
    for stale in OUT_DIR.glob("*.tga"):
        if stale.name not in keep:
            stale.unlink()
            print(f"  removed {stale.name} (source image is gone)")


def main():
    SOURCE_DIR.mkdir(parents=True, exist_ok=True)
    OUT_DIR.mkdir(parents=True, exist_ok=True)

    overrides = json.loads(OVERRIDES.read_text(encoding="utf-8")) if OVERRIDES.exists() else {}
    sources = sorted((p for p in SOURCE_DIR.iterdir()
                      if p.is_file() and p.suffix.lower() in SOURCE_EXT),
                     key=lambda p: p.stem.lower())

    if not sources:
        # Still emit valid, empty Lua so the addon loads instead of pointing at
        # textures that are no longer on disk.
        write_lua([])
        write_manifest([])
        prune(set())
        print(f"No images found in {SOURCE_DIR}")
        print("Drop some .gif / .png / .webp files in there and run this again.")
        return 0

    taken = base_trigger_words()
    entries, warnings, renamed, used = [], [], [], {}

    for src in sources:
        name, cfg = resolve_name(src, overrides)
        if name is None:
            warnings.append(f"{src.name}: skipped, {cfg}")
            continue
        if name in used:
            warnings.append(f"{src.name}: skipped, the name '{name}' is already taken by {used[name]}")
            continue

        try:
            entry = build_one(src, name, cfg)
        except Exception as exc:
            warnings.append(f"{src.name}: skipped ({exc})")
            continue

        used[name] = src.name
        entries.append(entry)
        if name != src.stem:
            renamed.append((src.stem, name))
        if name in taken:
            warnings.append(f"{name}: shadows an emote from the base pack")
        warnings += [f"{name}: alias '{a}' shadows a base-pack emote"
                     for a in entry["aliases"] if a in taken]

    write_lua(entries)
    write_manifest(entries)
    prune({e["file"] for e in entries})

    print(f"\n{len(entries)} emote(s) -> {OUT_DIR}")
    print(f"Generated {LUA_OUT}")
    if renamed:
        print("\nRenamed so chat can match them -- type the name on the right:")
        for was, now in renamed:
            print(f"  {was:<28} ->  {now}")
    if warnings:
        print("\nWarnings:")
        for w in warnings:
            print(f"  ! {w}")
    print("\nRestart WoW (or /reload if the addon was already loaded once).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
