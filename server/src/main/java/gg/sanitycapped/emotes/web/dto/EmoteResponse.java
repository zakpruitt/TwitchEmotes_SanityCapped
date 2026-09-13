package gg.sanitycapped.emotes.web.dto;

import java.time.Duration;
import java.time.Instant;

import gg.sanitycapped.emotes.emote.Emote;
import gg.sanitycapped.emotes.emote.EmoteStatus;

public record EmoteResponse(String id, String name, String url, String uploader,
                            EmoteStatus status, boolean animated, String note, String age) {

    public static EmoteResponse from(Emote emote) {
        return new EmoteResponse(emote.id(), emote.name(), "/img/" + emote.fileName(),
                emote.uploader(), emote.status(), emote.animated(), emote.note(),
                age(emote.createdAt()));
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
