"""How a file name becomes the word people type in game.

Shared by build_emotes.py and build_sounds.py, and mirrored in the site's
Naming.java and static/js/naming.js. Change all of them together.
"""

import re

PREFIX = "sc"

# Emoticons_InsertEmoticons splits chat on these, so a name containing one can
# never match in chat (it still shows in autocomplete, which is the confusing bit).
CHAT_DELIMITERS = re.compile(r"[\s,'<>?\-.!]+")
SIZE_SUFFIX = re.compile(r"[-_](?:\d{1,4}px|\d{1,4}x|\d{1,4})$", re.I)

# A sound's name ends up in a slash command, so only letters and digits survive.
NOT_SLASH_SAFE = re.compile(r"[^A-Za-z0-9]+")


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


def sound_name(stem):
    """vine boom!.mp3 -> scVineboom; the slash command is that, lowercased."""
    return apply_prefix(NOT_SLASH_SAFE.sub("", sanitize(stem)))
