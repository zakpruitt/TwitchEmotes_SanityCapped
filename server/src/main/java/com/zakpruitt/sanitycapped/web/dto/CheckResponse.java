package com.zakpruitt.sanitycapped.web.dto;

import com.zakpruitt.sanitycapped.emote.DuplicateCheck;
import com.zakpruitt.sanitycapped.emote.EmoteStatus;

public record CheckResponse(String name, EmoteStatus nameTaken, String exact) {

    public static CheckResponse from(DuplicateCheck check) {
        return new CheckResponse(check.name(), check.nameTaken(), check.exact());
    }
}
