package gg.sanitycapped.emotes.emote;

import java.time.Instant;
import java.util.UUID;

import gg.sanitycapped.emotes.image.ImageInfo;

/**
 * One emote, at whatever stage it has reached.
 *
 * @param name       the trigger word, which is also the filename in the repo
 * @param sha256     identity of the bytes, for catching re-uploads
 * @param dhash      what it looks like, for catching near-duplicates
 * @param note       a warning for the queue, or a rejection reason
 * @param githubSha  blob sha of the committed source image, needed to delete it
 */
public record Emote(
        String id,
        String name,
        String ext,
        String sha256,
        String dhash,
        String uploader,
        EmoteStatus status,
        String note,
        boolean animated,
        long bytes,
        Instant createdAt,
        Instant decidedAt,
        String githubSha) {

    /** A freshly uploaded emote, waiting in the queue. */
    public static Emote pending(String name, byte[] data, ImageInfo info, String uploader,
                                String sha256, String note, Instant now) {
        return new Emote(UUID.randomUUID().toString(), name, info.extension(), sha256, info.dhash(),
                uploader, EmoteStatus.PENDING, note, info.animated(), data.length, now, null, null);
    }

    /** An emote already in the repo, adopted by the startup sync. */
    public static Emote adopted(String name, byte[] data, ImageInfo info, String sha256,
                                String githubSha, Instant now) {
        return new Emote(UUID.randomUUID().toString(), name, info.extension(), sha256, info.dhash(),
                "the vault", EmoteStatus.APPROVED, null, info.animated(), data.length,
                now, now, githubSha);
    }

    /** Where the original is kept on disk, and the URL it is served at. */
    public String fileName() {
        return id + "." + ext;
    }

    /** What the image is called in the repo: the trigger word, so the build renames nothing. */
    public String sourceFileName() {
        return name + "." + ext;
    }

    public boolean isApproved() {
        return status == EmoteStatus.APPROVED;
    }
}
