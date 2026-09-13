package com.zakpruitt.sanitycapped.web.dto.response;

import com.zakpruitt.sanitycapped.emote.model.Emote;

import java.util.List;

public record PendingQueueResponse(List<EmoteResponse> pending) {

    public static PendingQueueResponse from(List<Emote> emotes) {
        return new PendingQueueResponse(emotes.stream().map(EmoteResponse::from).toList());
    }
}
