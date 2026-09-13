package com.zakpruitt.sanitycapped.web.dto.response;

import com.zakpruitt.sanitycapped.emote.Emote;

import java.util.List;

public record EmoteListResponse(List<EmoteResponse> emotes) {

    public static EmoteListResponse from(List<Emote> emotes) {
        return new EmoteListResponse(emotes.stream().map(EmoteResponse::from).toList());
    }
}
