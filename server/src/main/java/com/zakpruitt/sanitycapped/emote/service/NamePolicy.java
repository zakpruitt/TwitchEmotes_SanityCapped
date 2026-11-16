package com.zakpruitt.sanitycapped.emote.service;

import com.zakpruitt.sanitycapped.emote.exception.EmoteException;
import com.zakpruitt.sanitycapped.naming.Naming;
import org.springframework.stereotype.Component;

/** Whether a typed name leaves a word someone can actually type in game. */
@Component
public class NamePolicy {

    private static final int MAX_LENGTH = 40;

    public String requireUsable(String typedName) {
        String name = Naming.triggerWord(typedName);
        if (name.isEmpty()) {
            throw new EmoteException.Invalid(
                    "That name is all characters chat breaks on, so nothing usable is left.");
        }
        return requireTypeable(name);
    }

    /** A sound's name becomes a slash command, so only letters and digits count. */
    public String requireUsableSound(String typedName) {
        String name = Naming.soundName(typedName);
        if (name.isEmpty()) {
            throw new EmoteException.Invalid(
                    "That name has no letters or digits, so there is no command to make from it.");
        }
        return requireTypeable(name);
    }

    private static String requireTypeable(String name) {
        if (name.length() > MAX_LENGTH) {
            throw new EmoteException.Invalid("That name is too long to type in chat.");
        }
        return name;
    }
}
