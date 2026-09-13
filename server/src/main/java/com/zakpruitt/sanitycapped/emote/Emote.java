package com.zakpruitt.sanitycapped.emote;

import com.zakpruitt.sanitycapped.image.ImageInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "emotes")
public class Emote {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String ext;

    @Column(nullable = false)
    private String sha256;

    private String dhash;

    @Column(nullable = false)
    private String uploader;

    @Column(nullable = false)
    private EmoteStatus status;

    private String note;

    @Column(nullable = false)
    private boolean animated;

    private long bytes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "github_sha")
    private String githubSha;

    private Emote(String name, String ext, String sha256, String dhash, String uploader,
                  EmoteStatus status, String note, boolean animated, long bytes,
                  Instant createdAt, Instant decidedAt, String githubSha) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.ext = ext;
        this.sha256 = sha256;
        this.dhash = dhash;
        this.uploader = uploader;
        this.status = status;
        this.note = note;
        this.animated = animated;
        this.bytes = bytes;
        this.createdAt = createdAt;
        this.decidedAt = decidedAt;
        this.githubSha = githubSha;
    }

    public static Emote pending(String name, byte[] data, ImageInfo info, String uploader,
                                String sha256, String note, Instant now) {
        return new Emote(name, info.extension(), sha256, info.dhash(), uploader,
                EmoteStatus.PENDING, note, info.animated(), data.length, now, null, null);
    }

    /** An emote already in the repo, picked up by RepoSync. */
    public static Emote adopted(String name, byte[] data, ImageInfo info, String sha256,
                                String githubSha, Instant now) {
        return new Emote(name, info.extension(), sha256, info.dhash(), "the vault",
                EmoteStatus.APPROVED, null, info.animated(), data.length, now, now, githubSha);
    }

    public void approve(String name, String githubSha, Instant at) {
        this.name = name;
        this.status = EmoteStatus.APPROVED;
        this.note = null;
        this.githubSha = githubSha;
        this.decidedAt = at;
    }

    public void reject(String reason, Instant at) {
        this.status = EmoteStatus.REJECTED;
        this.note = reason;
        this.decidedAt = at;
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
