package com.zakpruitt.sanitycapped.web.dto.response;

import com.zakpruitt.sanitycapped.emote.model.Emote;

public record UploadResponse(String id, String name, String url, String note) {

    public static UploadResponse from(Emote emote) {
        return new UploadResponse(emote.getId(), emote.getName(), "/img/" + emote.fileName(),
                emote.getNote());
    }
}
