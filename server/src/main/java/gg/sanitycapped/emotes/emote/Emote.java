package gg.sanitycapped.emotes.emote;

import java.time.Instant;
import java.util.UUID;

import gg.sanitycapped.emotes.image.ImageInfo;

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

    public static Emote pending(String name, byte[] data, ImageInfo info, String uploader,
                                String sha256, String note, Instant now) {
        return new Emote(UUID.randomUUID().toString(), name, info.extension(), sha256, info.dhash(),
                uploader, EmoteStatus.PENDING, note, info.animated(), data.length, now, null, null);
    }

    /** An emote already in the repo, picked up by RepoSync. */
    public static Emote adopted(String name, byte[] data, ImageInfo info, String sha256,
                                String githubSha, Instant now) {
        return new Emote(UUID.randomUUID().toString(), name, info.extension(), sha256, info.dhash(),
                "the vault", EmoteStatus.APPROVED, null, info.animated(), data.length,
                now, now, githubSha);
    }

    public String fileName() {
        return id + "." + ext;
    }

    /** Named after the trigger word in the repo, so the build renames nothing. */
    public String sourceFileName() {
        return name + "." + ext;
    }

    public boolean isApproved() {
        return status == EmoteStatus.APPROVED;
    }
}
