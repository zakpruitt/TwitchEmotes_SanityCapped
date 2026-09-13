package com.zakpruitt.sanitycapped.image;

import java.util.Arrays;
import java.util.Optional;

public enum ImageType {

    GIF("gif", "image/gif", 0x47, 0x49, 0x46, 0x38),
    PNG("png", "image/png", 0x89, 0x50, 0x4E, 0x47),
    JPEG("jpg", "image/jpeg", 0xFF, 0xD8, 0xFF),
    WEBP("webp", "image/webp", 0x52, 0x49, 0x46, 0x46);

    private final String extension;
    private final String contentType;
    private final int[] signature;

    ImageType(String extension, String contentType, int... signature) {
        this.extension = extension;
        this.contentType = contentType;
        this.signature = signature;
    }

    /**
     * A renamed .exe is not an emote, so the type comes from the bytes.
     */
    public static Optional<ImageType> sniff(byte[] data) {
        return Arrays.stream(values())
                .filter(type -> type.matches(data))
                .findFirst();
    }

    public static String contentTypeOf(String fileName) {
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        return Arrays.stream(values())
                .filter(type -> type.extension.equals(ext) || ("jpeg".equals(ext) && type == JPEG))
                .findFirst()
                .map(ImageType::contentType)
                .orElse("application/octet-stream");
    }

    public String extension() {
        return extension;
    }

    public String contentType() {
        return contentType;
    }

    private boolean matches(byte[] data) {
        if (data.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((data[i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        // RIFF alone is any RIFF container; WEBP is spelled out at offset 8.
        return this != WEBP || (data.length > 12
                && data[8] == 'W' && data[9] == 'E' && data[10] == 'B' && data[11] == 'P');
    }
}
