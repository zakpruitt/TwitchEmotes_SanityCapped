package gg.sanitycapped.emotes.store;

public record Emote(
        String id,
        String name,
        String ext,
        String sha256,
        String dhash,
        String uploader,
        String status,
        String note,
        boolean animated,
        long bytes,
        long createdAt,
        Long decidedAt,
        String githubSha) {

    public static final String PENDING = "pending";
    public static final String APPROVED = "approved";
    public static final String REJECTED = "rejected";

    public String fileName() {
        return id + "." + ext;
    }
}
