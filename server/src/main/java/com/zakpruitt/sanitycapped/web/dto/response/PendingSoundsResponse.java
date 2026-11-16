package com.zakpruitt.sanitycapped.web.dto.response;

import com.zakpruitt.sanitycapped.sound.model.Sound;

import java.util.List;

public record PendingSoundsResponse(List<SoundResponse> pending) {

    public static PendingSoundsResponse from(List<Sound> sounds) {
        return new PendingSoundsResponse(sounds.stream().map(SoundResponse::from).toList());
    }
}
