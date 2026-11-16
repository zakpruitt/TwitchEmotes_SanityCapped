package com.zakpruitt.sanitycapped.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.zakpruitt.sanitycapped.config.AppProperties;
import com.zakpruitt.sanitycapped.github.dto.Release;
import com.zakpruitt.sanitycapped.github.dto.SourceFile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The only outbound calls. Commits emote images into the addon repo's
 * tools/source/ and MP3s into the sound pack's Sounds/, either of which
 * triggers a release build.
 */
@Component
public class GitHubClient {

    private static final String SOURCE_PATH = "/contents/tools/source";
    private static final String SOUND_PATH = "/contents/Soundboard_SanityCapped/Sounds";

    private final AppProperties props;
    private final RestClient api;
    private final RestClient raw;

    GitHubClient(AppProperties props, RestClient.Builder builder) {
        this.props = props;
        this.api = apiClient(props, builder.clone());
        this.raw = builder.clone().build();
    }

    public List<SourceFile> listSource() {
        return list(sourcePath());
    }

    public List<SourceFile> listSounds() {
        return list(soundPath());
    }

    public byte[] download(String url) {
        return raw.get().uri(url).retrieve().body(byte[].class);
    }

    /** @return the new blob sha, which a later delete has to quote */
    public String putSource(String fileName, byte[] content, String message) {
        return put(sourcePath(), fileName, content, message);
    }

    /** @return the new blob sha, which a later delete has to quote */
    public String putSound(String fileName, byte[] content, String message) {
        return put(soundPath(), fileName, content, message);
    }

    public void deleteSource(String fileName, String sha, String message) {
        delete(sourcePath(), fileName, sha, message);
    }

    public void deleteSound(String fileName, String sha, String message) {
        delete(soundPath(), fileName, sha, message);
    }

    public Optional<String> shaFor(String fileName) {
        return sha(sourcePath(), fileName);
    }

    public Optional<String> soundShaFor(String fileName) {
        return sha(soundPath(), fileName);
    }

    public Optional<Release> latestRelease() {
        try {
            ReleaseEntry entry = api.get()
                    .uri(repoPath("/releases/latest"))
                    .retrieve()
                    .body(ReleaseEntry.class);
            return Optional.ofNullable(entry)
                    .map(e -> new Release(e.tagName(), e.htmlUrl(), e.publishedAt()));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    /** Empty when the folder is not in the repo yet. */
    private List<SourceFile> list(String folder) {
        List<ContentEntry> entries;
        try {
            entries = api.get()
                    .uri(folder)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ContentEntry>>() {
                    });
        } catch (HttpClientErrorException.NotFound e) {
            return List.of();
        }

        return entries == null ? List.of() : entries.stream()
                .filter(entry -> "file".equals(entry.type()))
                .map(entry -> new SourceFile(entry.name(), entry.sha(), entry.downloadUrl()))
                .toList();
    }

    private String put(String folder, String fileName, byte[] content, String message) {
        CommitResult result = api.put()
                .uri(folder + "/{file}", fileName)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("message", message,
                        "content", Base64.getEncoder().encodeToString(content)))
                .retrieve()
                .body(CommitResult.class);

        return result.content().sha();
    }

    private void delete(String folder, String fileName, String sha, String message) {
        api.method(HttpMethod.DELETE)
                .uri(folder + "/{file}", fileName)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("message", message, "sha", sha))
                .retrieve()
                .toBodilessEntity();
    }

    private Optional<String> sha(String folder, String fileName) {
        try {
            ContentEntry entry = api.get()
                    .uri(folder + "/{file}", fileName)
                    .retrieve()
                    .body(ContentEntry.class);
            return Optional.ofNullable(entry).map(ContentEntry::sha);
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private static RestClient apiClient(AppProperties props, RestClient.Builder builder) {
        builder.baseUrl("https://api.github.com")
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .defaultHeader("User-Agent", "sanity-capped-emotes");

        if (props.canPublish()) {
            builder.defaultHeader("Authorization", "Bearer " + props.githubToken());
        }
        return builder.build();
    }

    private String sourcePath() {
        return repoPath(SOURCE_PATH);
    }

    private String soundPath() {
        return repoPath(SOUND_PATH);
    }

    /** owner/name goes in the literal path: a template variable would encode the slash. */
    private String repoPath(String suffix) {
        return "/repos/" + props.githubRepo() + suffix;
    }

    record ContentEntry(String type, String name, String sha,
                        @JsonProperty("download_url") String downloadUrl) {
    }

    record CommitResult(ContentEntry content) {
    }

    record ReleaseEntry(@JsonProperty("tag_name") String tagName,
                        @JsonProperty("html_url") String htmlUrl,
                        @JsonProperty("published_at") String publishedAt) {
    }
}
