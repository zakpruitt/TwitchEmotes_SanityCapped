package com.zakpruitt.sanitycapped.web.dto.response;

import com.zakpruitt.sanitycapped.emote.model.EmoteStatus;
import com.zakpruitt.sanitycapped.sound.model.Sound;

public record SoundResponse(String id, String name, String command, String url, String uploader,
                            EmoteStatus status, double seconds, String note, String age) {

    public static SoundResponse from(Sound sound) {
        return new SoundResponse(sound.getId(), sound.getName(), sound.command(),
                "/snd/" + sound.fileName(), sound.getUploader(), sound.getStatus(),
                Math.round(sound.seconds() * 10) / 10.0, sound.getNote(),
                EmoteResponse.age(sound.getCreatedAt()));
    }
}
