package com.zakpruitt.sanitycapped.audio.dto;

public record AudioInfo(long durationMillis) {

    public double seconds() {
        return durationMillis / 1000.0;
    }
}
