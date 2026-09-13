package com.zakpruitt.sanitycapped.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.zakpruitt.sanitycapped.config.AppProperties;
import com.zakpruitt.sanitycapped.github.dto.Release;
import com.zakpruitt.sanitycapped.github.dto.SourceFile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The only outbound calls. Commits into the addon repo's tools/source/, which
 * is what triggers a release build.
 */
@Component
public class GitHubClient {

    private static final String SOURCE_PATH = "/contents/tools/source";

    private final AppProperties props;
    private final RestClient api;
    private final RestClient raw;

    GitHubClient(AppProperties props, RestClient.Builder builder) {
        this.props = props;
        this.api = apiClient(props, builder.clone());
        this.raw = builder.clone().build();
    }

    public List<SourceFile> listSource() {
        List<ContentEntry> entries = api.get()
                .uri(sourcePath())
                .retrieve()
                .body(new ParameterizedTypeReference<List<ContentEntry>>() {
                });

        return entries == null ? List.of() : entries.stream()
                .filter(entry -> "file".equals(entry.type()))
                .map(entry -> new SourceFile(entry.name(), entry.sha(), entry.downloadUrl()))
                .toList();
    }

    public byte[] download(String url) {
        return raw.get().uri(url).retrieve().body(byte[].class);
    }

    /** @return the new blob sha, which a later delete has to quote */
    public String putSource(String fileName, byte[] content, String message) {
        CommitResult result = api.put()
                .uri(sourcePath() + "/{file}", fileName)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("message", message,
                        "content", Base64.getEncoder().encodeToString(content)))
                .retrieve()
                .body(CommitResult.class);

        return result.content().sha();
    }

    public void deleteSource(String fileName, String sha, String message) {
        api.method(HttpMethod.DELETE)
                .uri(sourcePath() + "/{file}", fileName)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("message", message, "sha", sha))
                .retrieve()
                .toBodilessEntity();
    }

    public Optional<String> shaFor(String fileName) {
        try {
            ContentEntry entry = api.get()
                    .uri(sourcePath() + "/{file}", fileName)
                    .retrieve()
                    .body(ContentEntry.class);
            return Optional.ofNullable(entry).map(ContentEntry::sha);
        } catch (RuntimeException e) {
            return Optional.empty();
        }
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
