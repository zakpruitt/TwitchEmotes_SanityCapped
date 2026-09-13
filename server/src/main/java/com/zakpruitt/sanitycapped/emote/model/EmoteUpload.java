package com.zakpruitt.sanitycapped.emote.model;

public record EmoteUpload(byte[] data, String requestedName, String originalFileName,
                          String uploader, String ip) {
}
