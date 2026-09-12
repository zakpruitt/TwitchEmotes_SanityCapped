package gg.sanitycapped.emotes.github;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import gg.sanitycapped.emotes.config.AppProperties;

/**
 * The only thing here that reaches outside the app. Approved emotes are
 * committed into the addon repo's tools/source/, and that push is what makes
 * GitHub Actions rebuild the textures and publish a release for WoWUp.
 */
@Component
public class GitHubClient {

    private static final String SOURCE_PATH = "/contents/tools/source";

    private final AppProperties props;
    private final RestClient api;
    private final RestClient raw;

    GitHubClient(AppProperties props, RestClient.Builder builder) {
        this.props = props;
        RestClient.Builder github = builder.clone()
                .baseUrl("https://api.github.com")
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .defaultHeader("User-Agent", "sanity-capped-emotes");
        if (props.canPublish()) {
            github = github.defaultHeader("Authorization", "Bearer " + props.githubToken());
        }
        this.api = github.build();
        this.raw = builder.clone().build();
    }

    /** A file in tools/source/, which is one emote's original image. */
    public record SourceFile(String name, String sha, String downloadUrl) {

        public String stem() {
            return name.replaceFirst("\\.[^.]+$", "");
        }
    }

    public record Release(String tag, String url, String publishedAt) {
    }

    /** Everything the pack is currently built from. */
    @SuppressWarnings("unchecked")
    public List<SourceFile> listSource() {
        List<Map<String, Object>> files = api.get()
                .uri(repoPath(SOURCE_PATH))
                .retrieve()
                .body(List.class);
        return files == null ? List.of() : files.stream()
                .filter(file -> "file".equals(file.get("type")))
                .map(file -> new SourceFile((String) file.get("name"), (String) file.get("sha"),
                        (String) file.get("download_url")))
                .toList();
    }

    public byte[] download(String url) {
        return raw.get().uri(url).retrieve().body(byte[].class);
    }

    /** @return the new blob sha, which a later delete has to quote */
    @SuppressWarnings("unchecked")
    public String putSource(String fileName, byte[] content, String message) {
        Map<String, Object> response = api.put()
                .uri(repoPath(SOURCE_PATH) + "/{file}", fileName)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("message", message,
                        "content", Base64.getEncoder().encodeToString(content)))
                .retrieve()
                .body(Map.class);
        return (String) ((Map<String, Object>) response.get("content")).get("sha");
    }

    public void deleteSource(String fileName, String sha, String message) {
        api.method(HttpMethod.DELETE)
                .uri(repoPath(SOURCE_PATH) + "/{file}", fileName)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("message", message, "sha", sha))
                .retrieve()
                .toBodilessEntity();
    }

    /** Emotes that predate the site have no recorded sha, so look theirs up. */
    @SuppressWarnings("unchecked")
    public Optional<String> shaFor(String fileName) {
        try {
            Map<String, Object> response = api.get()
                    .uri(repoPath(SOURCE_PATH) + "/{file}", fileName)
                    .retrieve()
                    .body(Map.class);
            return Optional.ofNullable((String) response.get("sha"));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    /** What WoWUp would install right now, or empty if nothing is published yet. */
    @SuppressWarnings("unchecked")
    public Optional<Release> latestRelease() {
        try {
            Map<String, Object> response = api.get()
                    .uri(repoPath("/releases/latest"))
                    .retrieve()
                    .body(Map.class);
            return Optional.of(new Release((String) response.get("tag_name"),
                    (String) response.get("html_url"), (String) response.get("published_at")));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    /**
     * The repo is owner/name, and a URI template variable would percent-encode
     * that slash into a 404, so it belongs in the literal path.
     */
    private String repoPath(String suffix) {
        return "/repos/" + props.githubRepo() + suffix;
    }
}
