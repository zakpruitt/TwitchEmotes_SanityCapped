package com.zakpruitt.sanitycapped.emote.service;

import com.zakpruitt.sanitycapped.emote.exception.EmoteException;
import com.zakpruitt.sanitycapped.naming.Naming;
import org.springframework.stereotype.Component;

/** Whether a typed name leaves a trigger word someone can actually use in chat. */
@Component
public class NamePolicy {

    private static final int MAX_LENGTH = 40;

    public String requireUsable(String typedName) {
        String name = Naming.triggerWord(typedName);
        if (name.isEmpty()) {
            throw new EmoteException.Invalid(
                    "That name is all characters chat breaks on, so nothing usable is left.");
        }
        if (name.length() > MAX_LENGTH) {
            throw new EmoteException.Invalid("That name is too long to type in chat.");
        }
        return name;
    }
}
