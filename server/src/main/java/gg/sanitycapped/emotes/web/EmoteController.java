package gg.sanitycapped.emotes.web;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import gg.sanitycapped.emotes.core.EmoteService;
import gg.sanitycapped.emotes.github.GitHubClient;
import gg.sanitycapped.emotes.store.Emote;

@RestController
public class EmoteController {

    private final EmoteService emotes;
    private final GitHubClient github;
    private final Passcodes passcodes;

    EmoteController(EmoteService emotes, GitHubClient github, Passcodes passcodes) {
        this.emotes = emotes;
        this.github = github;
        this.passcodes = passcodes;
    }

    @GetMapping("/api/emotes")
    public Map<String, List<EmoteView>> approved() {
        return Map.of("emotes", emotes.approved().stream().map(EmoteView::of).toList());
    }

    @GetMapping("/api/check")
    public EmoteService.Check check(@RequestParam(defaultValue = "") String name,
                                    @RequestParam(defaultValue = "") String sha) {
        return emotes.check(name, sha);
    }

    @PostMapping(value = "/api/emotes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> upload(@RequestParam MultipartFile file,
                                                      @RequestParam(defaultValue = "") String name,
                                                      @RequestParam(defaultValue = "") String uploader,
                                                      HttpServletRequest request) throws IOException {
        passcodes.requireGuild(request);
        if (file.isEmpty()) {
            throw ApiException.badRequest("No file received.");
        }

        String typed = name.isBlank() ? stem(file.getOriginalFilename()) : name;
        Emote saved = emotes.upload(file.getBytes(), typed, uploader, clientIp(request));

        return ResponseEntity.status(201).body(Map.of(
                "ok", true,
                "id", saved.id(),
                "name", saved.name(),
                "url", "/img/" + saved.fileName(),
                "note", saved.note() == null ? "" : saved.note()));
    }

    @GetMapping("/api/session")
    public Map<String, Boolean> session(HttpServletRequest request) {
        return Map.of("guild", passcodes.isGuild(request), "admin", passcodes.isAdmin(request));
    }

    @GetMapping("/api/status")
    public Map<String, Object> status() {
        return github.latestRelease()
                .<Map<String, Object>>map(r -> Map.of(
                        "repoUrl", github.repoUrl(),
                        "release", Map.of("tag", r.tag(), "url", r.url(), "publishedAt", r.publishedAt())))
                .orElseGet(() -> Map.of("repoUrl", github.repoUrl()));
    }

    /** Uploaded originals, served straight off this machine's disk. */
    @GetMapping("/img/{fileName}")
    public ResponseEntity<byte[]> image(@PathVariable String fileName) {
        return emotes.image(fileName)
                .map(data -> ResponseEntity.ok()
                        .header("Content-Type", contentType(fileName))
                        // The name contains a uuid, so a given URL never changes content.
                        .header("Cache-Control", "public, max-age=31536000, immutable")
                        .body(data))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private static String contentType(String fileName) {
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        return switch (ext) {
            case "gif" -> "image/gif";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "jpg", "jpeg" -> "image/jpeg";
            default -> "application/octet-stream";
        };
    }

    private static String stem(String fileName) {
        return fileName == null ? "" : fileName.replaceFirst("\\.[^.]+$", "");
    }

    /** Behind a proxy (Fly, Render) the real address is in X-Forwarded-For. */
    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String remote = request.getRemoteAddr();
        return remote == null ? "unknown" : remote;
    }
}
