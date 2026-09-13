package com.zakpruitt.sanitycapped.emote.model;

import com.zakpruitt.sanitycapped.emote.EmoteStatus;

public record DuplicateCheck(String name, EmoteStatus nameTaken, String exact) {
}
