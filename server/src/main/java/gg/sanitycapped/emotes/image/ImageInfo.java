package gg.sanitycapped.emotes.image;

/** What we learn by decoding an upload, rather than trusting what it is called. */
public record ImageInfo(ImageType type, int width, int height, int frames, String dhash) {

    public boolean animated() {
        return frames > 1;
    }

    public String extension() {
        return type.extension();
    }
}
