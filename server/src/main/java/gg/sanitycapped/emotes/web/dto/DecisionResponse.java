package gg.sanitycapped.emotes.web.dto;

import gg.sanitycapped.emotes.emote.Emote;
import gg.sanitycapped.emotes.emote.EmoteStatus;

public record DecisionResponse(String name, EmoteStatus status) {

    public static DecisionResponse from(Emote emote) {
        return new DecisionResponse(emote.name(), emote.status());
    }

    public static DecisionResponse rejected(String name) {
        return new DecisionResponse(name, EmoteStatus.REJECTED);
    }
}
