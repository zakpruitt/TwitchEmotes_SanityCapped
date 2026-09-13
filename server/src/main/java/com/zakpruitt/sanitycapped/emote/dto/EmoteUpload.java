package com.zakpruitt.sanitycapped.emote.dto;

public record EmoteUpload(byte[] data, String requestedName, String originalFileName,
                          String uploader, String ip) {
}
