package gg.sanitycapped.emotes.web.dto;

import java.util.List;

import gg.sanitycapped.emotes.emote.Emote;

public record PendingQueueResponse(List<EmoteResponse> pending) {

    public static PendingQueueResponse from(List<Emote> emotes) {
        return new PendingQueueResponse(emotes.stream().map(EmoteResponse::from).toList());
    }
}
