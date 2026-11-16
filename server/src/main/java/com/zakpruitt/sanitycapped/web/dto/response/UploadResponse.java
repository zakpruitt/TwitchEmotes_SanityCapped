package com.zakpruitt.sanitycapped.web.dto.response;

import com.zakpruitt.sanitycapped.emote.model.Emote;
import com.zakpruitt.sanitycapped.sound.model.Sound;

public record UploadResponse(String id, String name, String url, String note) {

    public static UploadResponse from(Emote emote) {
        return new UploadResponse(emote.getId(), emote.getName(), "/img/" + emote.fileName(),
                emote.getNote());
    }

    public static UploadResponse from(Sound sound) {
        return new UploadResponse(sound.getId(), sound.getName(), "/snd/" + sound.fileName(),
                sound.getNote());
    }
}
