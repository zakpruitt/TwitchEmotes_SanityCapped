package gg.sanitycapped.emotes.web.dto;

import gg.sanitycapped.emotes.emote.Emote;

public record UploadResponse(String id, String name, String url, String note) {

    public static UploadResponse from(Emote emote) {
        return new UploadResponse(emote.id(), emote.name(), "/img/" + emote.fileName(), emote.note());
    }
}
