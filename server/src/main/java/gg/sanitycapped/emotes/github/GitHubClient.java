package gg.sanitycapped.emotes.github;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import gg.sanitycapped.emotes.AppProperties;

/**
 * The only thing that reaches outside this app: committing approved emotes into
 * tools/source/, which is what makes GitHub Actions build and publish a release.
 */
@Component
public class GitHubClient {

    private final AppProperties props;
    private final RestClient http;

    GitHubClient(AppProperties props, RestClient.Builder builder) {
        this.props = props;
        RestClient.Builder b = builder
                .baseUrl("https://api.github.com")
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .defaultHeader("User-Agent", "sanity-capped-emotes");
        if (props.canPublish()) {
            b = b.defaultHeader("Authorization", "Bearer " + props.githubToken());
        }
        this.http = b.build();
    }

    public record SourceFile(String name, String sha, String downloadUrl, long size) {
    }

    public record Release(String tag, String url, String publishedAt) {
    }

    /** Everything currently in tools/source/ — the pack's real source of truth. */
    public List<SourceFile> listSource() {
        List<Map<String, Object>> files = http.get()
                .uri(contents(""))
                .retrieve()
                .body(List.class);
        if (files == null) {
            return List.of();
        }
        return files.stream()
                .filter(f -> "file".equals(f.get("type")))
                .map(f -> new SourceFile((String) f.get("name"), (String) f.get("sha"),
                        (String) f.get("download_url"), ((Number) f.getOrDefault("size", 0)).longValue()))
                .toList();
    }

    public byte[] download(String url) {
        return RestClient.create().get().uri(url).retrieve().body(byte[].class);
    }

    /** Returns the new blob sha, which is what a later delete has to quote. */
    public String putSource(String fileName, byte[] content, String message) {
        Map<String, Object> body = http.put()
                .uri(contents("/{file}"), fileName)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("message", message, "content", Base64.getEncoder().encodeToString(content)))
                .retrieve()
                .body(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> content0 = (Map<String, Object>) body.get("content");
        return (String) content0.get("sha");
    }

    public void deleteSource(String fileName, String sha, String message) {
        http.method(org.springframework.http.HttpMethod.DELETE)
                .uri(contents("/{file}"), fileName)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("message", message, "sha", sha))
                .retrieve()
                .toBodilessEntity();
    }

    /** Emotes that predate the site have no recorded sha, so look it up. */
    public Optional<String> shaFor(String fileName) {
        try {
            Map<String, Object> body = http.get()
                    .uri(contents("/{file}"), fileName)
                    .retrieve()
                    .body(Map.class);
            return Optional.ofNullable((String) body.get("sha"));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    public Optional<Release> latestRelease() {
        try {
            Map<String, Object> r = http.get()
                    .uri("/repos/" + props.githubRepo() + "/releases/latest")
                    .retrieve()
                    .body(Map.class);
            return Optional.of(new Release((String) r.get("tag_name"), (String) r.get("html_url"),
                    (String) r.get("published_at")));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private String contents(String suffix) {
        return "/repos/" + props.githubRepo() + "/contents/tools/source" + suffix;
    }

    public String repoUrl() {
        return "https://github.com/" + props.githubRepo();
    }
}
