package gg.sanitycapped.emotes.emote;

public record EmoteUpload(byte[] data, String requestedName, String originalFileName,
                          String uploader, String ip) {
}
