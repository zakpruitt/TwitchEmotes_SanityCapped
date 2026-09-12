package gg.sanitycapped.emotes.core;

/** What we learn by actually decoding an upload, rather than trusting its name. */
public record ImageInfo(String contentType, String ext, int width, int height,
                        int frames, String dhash) {

    public boolean animated() {
        return frames > 1;
    }
}
