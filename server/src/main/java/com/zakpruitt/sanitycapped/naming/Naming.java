package com.zakpruitt.sanitycapped.naming;

import java.util.regex.Pattern;

/**
 * Ported from sanitize()/apply_prefix() in tools/build_emotes.py, and mirrored
 * again in static/js/naming.js. All three must agree or the site promises a
 * trigger word the addon build will not produce; NamingTest pins this one.
 */
public final class Naming {

    private static final String PREFIX = "sc";

    /**
     * TwitchEmotes splits chat on these, so a name containing one never matches.
     */
    private static final Pattern CHAT_DELIMITERS = Pattern.compile("[\\s,'<>?\\-.!]+");

    /**
     * 7TV and BTTV hand out files like peepoHmm-128.png and catJAM_2x.gif.
     */
    private static final Pattern SIZE_SUFFIX =
            Pattern.compile("[-_](?:\\d{1,4}px|\\d{1,4}x|\\d{1,4})$", Pattern.CASE_INSENSITIVE);

    private Naming() {
    }

    /**
     * peepoHmm-128.png -> peepoHmm-128.
     */
    public static String stem(String fileName) {
        return fileName == null ? "" : fileName.replaceFirst("\\.[^.]+$", "");
    }

    public static String triggerWord(String stem) {
        return applyPrefix(sanitize(stem));
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

    /**
     * cloudzUlt -> scCloudzUlt, FUCK -> scFuck, scFoo -> scFoo.
     */
    public static String applyPrefix(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        if (alreadyPrefixed(name)) {
            return name;
        }

        String core = shouting(name)
                ? Character.toUpperCase(name.charAt(0)) + name.substring(1).toLowerCase()
                : Character.toUpperCase(name.charAt(0)) + name.substring(1);
        return PREFIX + core;
    }

    /**
     * Guards against rebuilds turning scSomething into scScSomething.
     */
    private static boolean alreadyPrefixed(String name) {
        return name.startsWith(PREFIX)
                && name.length() > PREFIX.length()
                && Character.isUpperCase(name.charAt(PREFIX.length()));
    }

    /**
     * scFUCK reads worse than scFuck, so all-caps names get title-cased.
     */
    private static boolean shouting(String name) {
        if (name.length() <= 1) {
            return false;
        }

        boolean sawLetter = false;
        for (char c : name.toCharArray()) {
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
