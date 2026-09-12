package gg.sanitycapped.emotes.web;

import java.time.Duration;
import java.time.Instant;

import gg.sanitycapped.emotes.emote.Emote;
import gg.sanitycapped.emotes.emote.EmoteStatus;

/**
 * One emote as the pages want it: a URL instead of a filename, an age instead of
 * a timestamp, and no hashes. Used by both Thymeleaf and the JSON endpoints.
 */
public record EmoteView(String id, String name, String url, String uploader,
                        EmoteStatus status, boolean animated, String note, String age) {

    public static EmoteView of(Emote emote) {
        return new EmoteView(emote.id(), emote.name(), "/img/" + emote.fileName(), emote.uploader(),
                emote.status(), emote.animated(), emote.note(), age(emote.createdAt()));
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
