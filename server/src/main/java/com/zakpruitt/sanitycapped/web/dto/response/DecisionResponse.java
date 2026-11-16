package com.zakpruitt.sanitycapped.web.dto.response;

import com.zakpruitt.sanitycapped.emote.model.Emote;
import com.zakpruitt.sanitycapped.emote.model.EmoteStatus;
import com.zakpruitt.sanitycapped.sound.model.Sound;

public record DecisionResponse(String name, EmoteStatus status) {

    public static DecisionResponse from(Emote emote) {
        return new DecisionResponse(emote.getName(), emote.getStatus());
    }

    public static DecisionResponse from(Sound sound) {
        return new DecisionResponse(sound.getName(), sound.getStatus());
    }
}
