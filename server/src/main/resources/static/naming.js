// Port of sanitize()/apply_prefix() from tools/build_emotes.py. The site must
// show the same trigger word the build will produce, or people name an emote
// one thing and type another in game. Imported by the browser and by the API.
const PREFIX = "sc";

// TwitchEmotes splits chat on these, so a name containing one can never match.
const CHAT_DELIMITERS = /[\s,'<>?\-.!]+/g;
const SIZE_SUFFIX = /[-_](?:\d{1,4}px|\d{1,4}x|\d{1,4})$/i;

export function sanitize(stem) {
  let name = String(stem || "");
  while (SIZE_SUFFIX.test(name)) name = name.replace(SIZE_SUFFIX, "");
  return name.replace(CHAT_DELIMITERS, "");
}

function isUpper(s) {
  return /[A-Za-z]/.test(s) && s === s.toUpperCase();
}

export function applyPrefix(name) {
  if (!PREFIX || !name) return name;
  const already =
    name.startsWith(PREFIX) &&
    name.length > PREFIX.length &&
    name[PREFIX.length] === name[PREFIX.length].toUpperCase() &&
    /[A-Z]/.test(name[PREFIX.length]);
  if (already) return name;
  const core =
    isUpper(name) && name.length > 1
      ? name[0].toUpperCase() + name.slice(1).toLowerCase()
      : name[0].toUpperCase() + name.slice(1);
  return PREFIX + core;
}

/** Final in-game trigger word for a filename stem or typed name. */
export function triggerWord(stem) {
  return applyPrefix(sanitize(stem));
}
