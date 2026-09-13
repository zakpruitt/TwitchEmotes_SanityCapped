package com.zakpruitt.sanitycapped.web.dto.response;

import com.zakpruitt.sanitycapped.emote.Emote;
import com.zakpruitt.sanitycapped.emote.EmoteStatus;

public record DecisionResponse(String name, EmoteStatus status) {

    public static DecisionResponse from(Emote emote) {
        return new DecisionResponse(emote.getName(), emote.getStatus());
    }

    public static DecisionResponse rejected(String name) {
        return new DecisionResponse(name, EmoteStatus.REJECTED);
    }
}
