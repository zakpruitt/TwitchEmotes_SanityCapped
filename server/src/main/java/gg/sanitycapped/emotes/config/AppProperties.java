package gg.sanitycapped.emotes.config;

import java.nio.file.Path;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Everything this app is configured with, all of it environment variables. */
@Validated
@ConfigurationProperties("app")
public record AppProperties(

        /** Holds the SQLite file and the uploaded images; needs to be a real volume. */
        @NotNull Path dataDir,

        /** Handed out in guild chat. Lets people upload. */
        @NotBlank String guildPasscode,

        /** Approve, reject, remove, resync. */
        @NotBlank String adminPasscode,

        /** owner/name of the addon repo approved emotes are committed to. */
        @NotBlank String githubRepo,

        /** Fine-grained PAT with Contents: read and write. Absent means read-only. */
        String githubToken,

        @Positive long maxUploadBytes,

        /** Differing dHash bits below which two images are called near-duplicates. */
        @Positive int similarThreshold,

        @Positive int uploadsPerHour) {

    public Path imageDir() {
        return dataDir.resolve("images");
    }

    /** Without a token the queue still works; approving has nowhere to commit. */
    public boolean canPublish() {
        return githubToken != null && !githubToken.isBlank();
    }

    public String repoUrl() {
        return "https://github.com/" + githubRepo;
    }

    public long maxUploadMegabytes() {
        return maxUploadBytes / 1024 / 1024;
    }
}
