package gg.sanitycapped.emotes.web.dto;

import java.util.List;

import gg.sanitycapped.emotes.emote.Emote;

public record EmoteListResponse(List<EmoteResponse> emotes) {

    public static EmoteListResponse from(List<Emote> emotes) {
        return new EmoteListResponse(emotes.stream().map(EmoteResponse::from).toList());
    }
}
