package gg.sanitycapped.emotes;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app")
public record AppProperties(
        Path dataDir,
        String guildPasscode,
        String adminPasscode,
        String githubRepo,
        String githubToken,
        long maxUploadBytes,
        int similarThreshold,
        int uploadsPerHour) {

    public Path imageDir() {
        return dataDir.resolve("images");
    }

    public boolean canPublish() {
        return githubToken != null && !githubToken.isBlank();
    }
}
