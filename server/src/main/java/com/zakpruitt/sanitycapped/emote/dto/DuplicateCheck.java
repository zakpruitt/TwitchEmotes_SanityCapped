package com.zakpruitt.sanitycapped.emote.dto;

import com.zakpruitt.sanitycapped.emote.model.EmoteStatus;

public record DuplicateCheck(String name, EmoteStatus nameTaken, String exact) {
}
