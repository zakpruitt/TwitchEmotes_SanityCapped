package gg.sanitycapped.emotes.core;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.sanitycapped.emotes.AppProperties;
import gg.sanitycapped.emotes.github.GitHubClient;
import gg.sanitycapped.emotes.store.Emote;
import gg.sanitycapped.emotes.store.EmoteRepository;
import gg.sanitycapped.emotes.store.ImageStore;
import gg.sanitycapped.emotes.web.ApiException;

@Service
public class EmoteService {

    private static final Logger log = LoggerFactory.getLogger(EmoteService.class);

    private final AppProperties props;
    private final EmoteRepository repo;
    private final ImageStore images;
    private final ImageInspector inspector;
    private final GitHubClient github;

    EmoteService(AppProperties props, EmoteRepository repo, ImageStore images,
                 ImageInspector inspector, GitHubClient github) {
        this.props = props;
        this.repo = repo;
        this.images = images;
        this.inspector = inspector;
        this.github = github;
    }

    public List<Emote> approved() {
        return repo.byStatus(Emote.APPROVED);
    }

    public List<Emote> pending() {
        return repo.pendingOldestFirst();
    }

    public List<Emote> everything() {
        return repo.all();
    }

    public Optional<byte[]> image(String fileName) {
        return images.get(fileName);
    }

    /** What the upload form asks while someone is still typing. */
    public record Check(String name, String nameTaken, String exact, String similar) {
    }

    public Check check(String typedName, String sha) {
        String name = Naming.triggerWord(typedName);
        String nameTaken = name.isEmpty() ? null
                : repo.liveByName(name).map(Emote::status).orElse(null);
        String exact = (sha == null || sha.isBlank()) ? null
                : repo.liveBySha(sha).map(Emote::name).orElse(null);
        return new Check(name, nameTaken, exact, null);
    }

    @Transactional
    public Emote upload(byte[] data, String typedName, String uploader, String ip) {
        long now = Instant.now().getEpochSecond();

        if (repo.uploadsSince(ip, now - 3600) >= props.uploadsPerHour()) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "That is " + props.uploadsPerHour() + " uploads in an hour. Give it a rest.");
        }
        if (uploader == null || uploader.isBlank()) {
            throw ApiException.badRequest("Add your name so we know who to thank.");
        }
        if (data.length > props.maxUploadBytes()) {
            throw ApiException.badRequest("That image is over "
                    + (props.maxUploadBytes() / 1024 / 1024) + " MB. Trim it down first.");
        }

        ImageInfo info = inspector.inspect(data);
        if (info == null) {
            throw ApiException.badRequest("That isn't a GIF, PNG, WebP or JPEG.");
        }

        String name = Naming.triggerWord(typedName);
        if (name.isEmpty()) {
            throw ApiException.badRequest(
                    "That name is all characters chat breaks on, so nothing usable is left.");
        }
        if (name.length() > 40) {
            throw ApiException.badRequest("That name is too long to type in chat.");
        }

        repo.liveByName(name).ifPresent(clash -> {
            throw ApiException.conflict(Emote.APPROVED.equals(clash.status())
                    ? name + " already exists in the pack."
                    : name + " is already waiting for approval.");
        });

        String sha = Hashes.sha256(data);
        repo.liveBySha(sha).ifPresent(dupe -> {
            throw ApiException.conflict("That exact image is already here as " + dupe.name() + ".");
        });

        // A near-match is a warning on the queue, not a block: a recoloured or
        // resized variant is sometimes exactly what someone meant to add.
        String note = nearestMatch(info.dhash(), null)
                .map(near -> "Looks a lot like " + near.name())
                .orElse(null);

        Emote emote = new Emote(UUID.randomUUID().toString(), name, info.ext(), sha, info.dhash(),
                uploader.trim(), Emote.PENDING, note, info.animated(), data.length, now, null, null);

        images.put(emote.fileName(), data);
        repo.insert(emote);
        repo.logUpload(ip, now);
        log.info("Queued {} from {} ({} bytes{})", name, uploader, data.length,
                info.animated() ? ", animated" : "");
        return emote;
    }

    private Optional<Emote> nearestMatch(String dhash, String ignoreId) {
        if (dhash == null) {
            return Optional.empty();
        }
        return repo.liveWithDhash().stream()
                .filter(e -> !e.id().equals(ignoreId))
                .filter(e -> Hashes.hamming(dhash, e.dhash()) <= props.similarThreshold())
                .findFirst();
    }

    /**
     * Commits the image into tools/source/. That push is what triggers the build
     * workflow, so approving here is the whole publish step.
     */
    @Transactional
    public Emote approve(String id, String renameTo) {
        Emote emote = require(id);
        if (Emote.APPROVED.equals(emote.status())) {
            return emote;
        }
        if (!props.canPublish()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No GitHub token configured, so nothing can be published.");
        }

        String name = emote.name();
        if (renameTo != null && !renameTo.isBlank()) {
            name = Naming.triggerWord(renameTo.trim());
            if (name.isEmpty()) {
                throw ApiException.badRequest("That rename leaves nothing chat can match.");
            }
            String wanted = name;
            repo.liveByName(wanted)
                    .filter(other -> !other.id().equals(id))
                    .ifPresent(other -> {
                        throw ApiException.conflict(wanted + " is already taken.");
                    });
        }

        byte[] data = images.get(emote.fileName())
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "The uploaded image is missing from storage."));

        String sha;
        try {
            // Naming the file after the trigger word keeps the build's own naming
            // pass a no-op, so what ships is what the site promised.
            sha = github.putSource(name + "." + emote.ext(), data,
                    "Add " + name + " (uploaded by " + emote.uploader() + ")");
        } catch (RuntimeException e) {
            log.warn("Commit of {} failed", name, e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Commit failed: " + rootMessage(e));
        }

        repo.approve(id, name, sha, Instant.now().getEpochSecond());
        log.info("Approved {}, committed as {}", emote.name(), name);
        return require(id);
    }

    @Transactional
    public void reject(String id, String reason) {
        Emote emote = require(id);
        if (Emote.APPROVED.equals(emote.status())) {
            throw ApiException.conflict("Already in the pack. Remove it instead.");
        }
        String trimmed = (reason == null || reason.isBlank()) ? null
                : reason.substring(0, Math.min(reason.length(), 200));
        repo.reject(id, trimmed, Instant.now().getEpochSecond());
    }

    /** Deleting the source image makes the next build drop its texture too. */
    @Transactional
    public void remove(String id) {
        Emote emote = require(id);

        if (Emote.APPROVED.equals(emote.status())) {
            String file = emote.name() + "." + emote.ext();
            String sha = emote.githubSha() != null ? emote.githubSha()
                    : github.shaFor(file).orElseThrow(() -> ApiException.conflict(
                            "That image isn't in the repo any more, so there is nothing to delete."));
            try {
                github.deleteSource(file, sha, "Remove " + emote.name());
            } catch (RuntimeException e) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "Delete failed: " + rootMessage(e));
            }
        }

        images.delete(emote.fileName());
        repo.delete(id);
        log.info("Removed {}", emote.name());
    }

    /**
     * Pulls tools/source/ down from the repo and adopts anything we don't know
     * about. Seeds the emotes that predate the site, and picks up any you add by
     * hand, so the repo stays the source of truth and this disk is a cache.
     */
    public int syncFromRepo() {
        int added = 0;
        for (GitHubClient.SourceFile file : github.listSource()) {
            String stem = file.name().replaceFirst("\\.[^.]+$", "");
            String name = Naming.triggerWord(stem);
            if (repo.liveByName(name).isPresent()) {
                continue;
            }
            byte[] data = github.download(file.downloadUrl());
            ImageInfo info = inspector.inspect(data);
            if (info == null) {
                log.warn("Skipping {}: not an image we can read", file.name());
                continue;
            }
            long now = Instant.now().getEpochSecond();
            Emote emote = new Emote(UUID.randomUUID().toString(), name, info.ext(),
                    Hashes.sha256(data), info.dhash(), "the vault", Emote.APPROVED, null,
                    info.animated(), data.length, now, now, file.sha());
            images.put(emote.fileName(), data);
            repo.insert(emote);
            added++;
        }
        if (added > 0) {
            log.info("Adopted {} emote(s) already in the repo", added);
        }
        return added;
    }

    private Emote require(String id) {
        return repo.byId(id).orElseThrow(() -> ApiException.notFound("No such emote."));
    }

    private static String rootMessage(RuntimeException e) {
        Throwable t = e;
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t.getMessage();
    }
}
