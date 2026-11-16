package com.zakpruitt.sanitycapped.web.dto.response;

import com.zakpruitt.sanitycapped.sound.model.Sound;

import java.util.List;

public record SoundListResponse(List<SoundResponse> sounds) {

    public static SoundListResponse from(List<Sound> sounds) {
        return new SoundListResponse(sounds.stream().map(SoundResponse::from).toList());
    }
}
