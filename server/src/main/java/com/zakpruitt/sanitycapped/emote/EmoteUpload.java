package com.zakpruitt.sanitycapped.emote;

public record EmoteUpload(byte[] data, String requestedName, String originalFileName,
                          String uploader, String ip) {
}
