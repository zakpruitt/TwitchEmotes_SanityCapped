package gg.sanitycapped.emotes.web.dto;

import gg.sanitycapped.emotes.emote.DuplicateCheck;
import gg.sanitycapped.emotes.emote.EmoteStatus;

public record CheckResponse(String name, EmoteStatus nameTaken, String exact) {

    public static CheckResponse from(DuplicateCheck check) {
        return new CheckResponse(check.name(), check.nameTaken(), check.exact());
    }
}
