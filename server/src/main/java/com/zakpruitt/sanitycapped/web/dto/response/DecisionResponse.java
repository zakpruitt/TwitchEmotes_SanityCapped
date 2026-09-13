package com.zakpruitt.sanitycapped.web.dto.response;

import com.zakpruitt.sanitycapped.emote.model.Emote;
import com.zakpruitt.sanitycapped.emote.model.EmoteStatus;

public record DecisionResponse(String name, EmoteStatus status) {

    public static DecisionResponse from(Emote emote) {
        return new DecisionResponse(emote.getName(), emote.getStatus());
    }
}
