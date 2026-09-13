package com.zakpruitt.sanitycapped.image.dto;

import com.zakpruitt.sanitycapped.image.ImageType;

public record ImageInfo(ImageType type, int width, int height, int frames, String dhash) {

    public boolean animated() {
        return frames > 1;
    }

    public String extension() {
        return type.extension();
    }
}
