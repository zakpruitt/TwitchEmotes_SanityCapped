package com.zakpruitt.sanitycapped.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;

@Validated
@ConfigurationProperties("app")
public record AppProperties(
        @NotNull Path dataDir,
        @NotBlank String guildPasscode,
        @NotBlank String adminPasscode,
        @NotBlank String githubRepo,
        String githubToken,
        @Positive long maxUploadBytes,
        @Positive int similarThreshold,
        @Positive int uploadsPerHour) {

    public Path imageDir() {
        return dataDir.resolve("images");
    }

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
