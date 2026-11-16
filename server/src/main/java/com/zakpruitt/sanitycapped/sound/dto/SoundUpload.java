package com.zakpruitt.sanitycapped.sound.dto;

public record SoundUpload(byte[] data, String requestedName, String originalFileName,
                          String uploader, String ip) {
}
