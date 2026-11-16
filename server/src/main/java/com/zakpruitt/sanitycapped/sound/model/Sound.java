package com.zakpruitt.sanitycapped.sound.model;

import com.zakpruitt.sanitycapped.audio.dto.AudioInfo;
import com.zakpruitt.sanitycapped.emote.model.EmoteStatus;
import com.zakpruitt.sanitycapped.naming.Naming;
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
@Table(name = "sounds")
public class Sound {

    private static final String EXT = "mp3";

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String sha256;

    @Column(nullable = false)
    private String uploader;

    @Column(nullable = false)
    private EmoteStatus status;

    private String note;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    private long bytes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "github_sha")
    private String githubSha;

    private Sound(String name, String sha256, String uploader, EmoteStatus status, long durationMs,
                  long bytes, Instant createdAt, Instant decidedAt, String githubSha) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.sha256 = sha256;
        this.uploader = uploader;
        this.status = status;
        this.durationMs = durationMs;
        this.bytes = bytes;
        this.createdAt = createdAt;
        this.decidedAt = decidedAt;
        this.githubSha = githubSha;
    }

    public static Sound pending(String name, byte[] data, AudioInfo info, String uploader,
                                String sha256, Instant now) {
        return new Sound(name, sha256, uploader, EmoteStatus.PENDING, info.durationMillis(),
                data.length, now, null, null);
    }

    /** A sound already in the repo, picked up by the repo sync. */
    public static Sound adopted(String name, byte[] data, AudioInfo info, String sha256,
                                String githubSha, Instant now) {
        return new Sound(name, sha256, "the vault", EmoteStatus.APPROVED, info.durationMillis(),
                data.length, now, now, githubSha);
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
        return id + "." + EXT;
    }

    /** Named after the sound in the repo, so the build renames nothing. */
    public String sourceFileName() {
        return name + "." + EXT;
    }

    /** What you type in game to play it. */
    public String command() {
        return Naming.command(name);
    }

    public double seconds() {
        return durationMs / 1000.0;
    }

    public boolean isApproved() {
        return status == EmoteStatus.APPROVED;
    }
}
