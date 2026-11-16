package com.zakpruitt.sanitycapped.web.dto.response;

import com.zakpruitt.sanitycapped.emote.model.EmoteStatus;
import com.zakpruitt.sanitycapped.emote.dto.DuplicateCheck;

/** For sounds, name is the display name and the command is that lowercased. */
public record CheckResponse(String name, EmoteStatus nameTaken, String exact) {

    public static CheckResponse from(DuplicateCheck check) {
        return new CheckResponse(check.name(), check.nameTaken(), check.exact());
    }
}
