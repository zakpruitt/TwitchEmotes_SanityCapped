package gg.sanitycapped.emotes.core;

import java.util.regex.Pattern;

/**
 * Port of sanitize()/apply_prefix() from tools/build_emotes.py, and of
 * static/naming.js, which shows the same thing while someone types. All three
 * have to agree or the site promises a trigger word the build doesn't produce.
 */
public final class Naming {

    private static final String PREFIX = "sc";

    /** TwitchEmotes splits chat on these, so a name containing one never matches. */
    private static final Pattern CHAT_DELIMITERS = Pattern.compile("[\\s,'<>?\\-.!]+");
    private static final Pattern SIZE_SUFFIX = Pattern.compile("[-_](?:\\d{1,4}px|\\d{1,4}x|\\d{1,4})$",
            Pattern.CASE_INSENSITIVE);

    private Naming() {
    }

    public static String sanitize(String stem) {
        if (stem == null) {
            return "";
        }
        String name = stem;
        while (SIZE_SUFFIX.matcher(name).find()) {
            name = SIZE_SUFFIX.matcher(name).replaceAll("");
        }
        return CHAT_DELIMITERS.matcher(name).replaceAll("");
    }

    public static String applyPrefix(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        boolean already = name.startsWith(PREFIX)
                && name.length() > PREFIX.length()
                && Character.isUpperCase(name.charAt(PREFIX.length()));
        if (already) {
            return name;
        }
        String core = isUpper(name) && name.length() > 1
                ? Character.toUpperCase(name.charAt(0)) + name.substring(1).toLowerCase()
                : Character.toUpperCase(name.charAt(0)) + name.substring(1);
        return PREFIX + core;
    }

    /** Final in-game trigger word for a filename stem or a typed name. */
    public static String triggerWord(String stem) {
        return applyPrefix(sanitize(stem));
    }

    private static boolean isUpper(String s) {
        boolean sawLetter = false;
        for (char c : s.toCharArray()) {
            if (Character.isLetter(c)) {
                sawLetter = true;
                if (Character.isLowerCase(c)) {
                    return false;
                }
            }
        }
        return sawLetter;
    }
}
