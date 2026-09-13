package com.zakpruitt.sanitycapped.web.dto.response;

import com.zakpruitt.sanitycapped.emote.model.Emote;
import com.zakpruitt.sanitycapped.emote.model.EmoteStatus;

import java.time.Duration;
import java.time.Instant;

public record EmoteResponse(String id, String name, String url, String uploader,
                            EmoteStatus status, boolean animated, String note, String age) {

    public static EmoteResponse from(Emote emote) {
        return new EmoteResponse(emote.getId(), emote.getName(), "/img/" + emote.fileName(),
                emote.getUploader(), emote.getStatus(), emote.isAnimated(), emote.getNote(),
                age(emote.getCreatedAt()));
    }

    private static String age(Instant when) {
        Duration ago = Duration.between(when, Instant.now());
        if (ago.toMinutes() < 1) {
            return "just now";
        }
        if (ago.toHours() < 1) {
            return ago.toMinutes() + "m ago";
        }
        if (ago.toDays() < 1) {
            return ago.toHours() + "h ago";
        }
        return ago.toDays() + "d ago";
    }
}
