package gg.sanitycapped.emotes.web;

import gg.sanitycapped.emotes.store.Emote;

/** The shape the pages consume. Keeps ids and hashes out of public responses. */
public record EmoteView(String id, String name, String url, String uploader,
                        String status, boolean animated, String note, long createdAt) {

    public static EmoteView of(Emote e) {
        return new EmoteView(e.id(), e.name(), "/img/" + e.fileName(), e.uploader(),
                e.status(), e.animated(), e.note(), e.createdAt());
    }
}
