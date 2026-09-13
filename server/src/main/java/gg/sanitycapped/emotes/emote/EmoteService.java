package gg.sanitycapped.emotes.emote;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.sanitycapped.emotes.config.AppProperties;
import gg.sanitycapped.emotes.github.GitHubClient;
import gg.sanitycapped.emotes.image.Hashes;
import gg.sanitycapped.emotes.image.ImageInfo;
import gg.sanitycapped.emotes.image.ImageInspector;
import gg.sanitycapped.emotes.naming.Naming;

@Service
public class EmoteService {

    private static final Logger log = LoggerFactory.getLogger(EmoteService.class);
    private static final int MAX_NAME_LENGTH = 40;
    private static final Duration RATE_WINDOW = Duration.ofHours(1);

    private final AppProperties props;
    private final EmoteRepository repository;
    private final ImageStore images;
    private final ImageInspector inspector;
    private final GitHubClient github;
    private final Clock clock;

    EmoteService(AppProperties props, EmoteRepository repository, ImageStore images,
                 ImageInspector inspector, GitHubClient github, Clock clock) {
        this.props = props;
        this.repository = repository;
        this.images = images;
        this.inspector = inspector;
        this.github = github;
        this.clock = clock;
    }

    public List<Emote> approved() {
        return repository.approvedNewestFirst();
    }

    public List<Emote> pending() {
        return repository.pendingOldestFirst();
    }

    public Optional<byte[]> image(String fileName) {
        return images.get(fileName);
    }

    public DuplicateCheck check(String typedName, String sha256) {
        String name = Naming.triggerWord(typedName);
        return new DuplicateCheck(
                name,
                name.isEmpty() ? null : repository.liveByName(name).map(Emote::status).orElse(null),
                sha256 == null || sha256.isBlank()
                        ? null : repository.liveBySha(sha256).map(Emote::name).orElse(null));
    }

    @Transactional
    public Emote upload(EmoteUpload upload) {
        Instant now = clock.instant();
        checkRateLimit(upload.ip(), now);

        if (upload.uploader() == null || upload.uploader().isBlank()) {
            throw new EmoteException.Invalid("Add your name so we know who to thank.");
        }
        if (upload.data().length > props.maxUploadBytes()) {
            throw new EmoteException.Invalid(
                    "That image is over %d MB. Trim it down first.".formatted(props.maxUploadMegabytes()));
        }

        ImageInfo info = inspector.inspect(upload.data())
                .orElseThrow(() -> new EmoteException.Invalid("That isn't a GIF, PNG, WebP or JPEG."));

        String name = requireUsableName(namedBy(upload));
        String sha256 = Hashes.sha256(upload.data());
        refuseDuplicates(name, sha256);

        Emote emote = Emote.pending(name, upload.data(), info, upload.uploader().trim(), sha256,
                nearDuplicateWarning(info.dhash()), now);

        images.put(emote.fileName(), upload.data());
        repository.insert(emote);
        repository.logUpload(upload.ip(), now);

        log.info("Queued {} from {} ({} bytes{})", emote.name(), emote.uploader(), emote.bytes(),
                emote.animated() ? ", animated" : "");
        return emote;
    }

    /** Committing the image is the publish step: the push is what builds the release. */
    @Transactional
    public Emote approve(String id, String renameTo) {
        Emote emote = require(id);
        if (emote.isApproved()) {
            return emote;
        }
        if (!props.canPublish()) {
            throw new EmoteException.PublishingDisabled(
                    "No GitHub token configured, so nothing can be published.");
        }

        String name = renameTo == null || renameTo.isBlank() ? emote.name() : rename(emote, renameTo);
        byte[] data = images.get(emote.fileName()).orElseThrow(() -> new EmoteException.MissingImage(
                "The uploaded image is missing from storage."));

        String githubSha = publish(name + "." + emote.ext(), data,
                "Add %s (uploaded by %s)".formatted(name, emote.uploader()));

        repository.approve(id, name, githubSha, clock.instant());
        log.info("Approved {}, committed as {}", emote.name(), name);
        return require(id);
    }

    @Transactional
    public String reject(String id, String reason) {
        Emote emote = require(id);
        if (emote.isApproved()) {
            throw new EmoteException.Duplicate("Already in the pack. Remove it instead.");
        }
        repository.reject(id, trimmed(reason), clock.instant());
        log.info("Rejected {}", emote.name());
        return emote.name();
    }

    /** Deleting the source image makes the next build drop its texture too. */
    @Transactional
    public void remove(String id) {
        Emote emote = require(id);
        if (emote.isApproved()) {
            unpublish(emote);
        }
        images.delete(emote.fileName());
        repository.delete(id);
        log.info("Removed {}", emote.name());
    }

    private static String namedBy(EmoteUpload upload) {
        if (upload.requestedName() != null && !upload.requestedName().isBlank()) {
            return upload.requestedName();
        }
        String fileName = upload.originalFileName();
        return fileName == null ? "" : fileName.replaceFirst("\\.[^.]+$", "");
    }

    private void checkRateLimit(String ip, Instant now) {
        if (repository.uploadsSince(ip, now.minus(RATE_WINDOW)) >= props.uploadsPerHour()) {
            throw new EmoteException.RateLimited(
                    "That is %d uploads in an hour. Give it a rest.".formatted(props.uploadsPerHour()));
        }
    }

    private String requireUsableName(String typedName) {
        String name = Naming.triggerWord(typedName);
        if (name.isEmpty()) {
            throw new EmoteException.Invalid(
                    "That name is all characters chat breaks on, so nothing usable is left.");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new EmoteException.Invalid("That name is too long to type in chat.");
        }
        return name;
    }

    private void refuseDuplicates(String name, String sha256) {
        repository.liveByName(name).ifPresent(clash -> {
            throw new EmoteException.Duplicate(clash.isApproved()
                    ? name + " already exists in the pack."
                    : name + " is already waiting for approval.");
        });
        repository.liveBySha(sha256).ifPresent(dupe -> {
            throw new EmoteException.Duplicate(
                    "That exact image is already here as " + dupe.name() + ".");
        });
    }

    /** Warned about rather than refused: a resized variant is sometimes the point. */
    private String nearDuplicateWarning(String dhash) {
        return repository.live().stream()
                .filter(other -> Hashes.hamming(dhash, other.dhash()) <= props.similarThreshold())
                .findFirst()
                .map(near -> "Looks a lot like " + near.name())
                .orElse(null);
    }

    private String rename(Emote emote, String renameTo) {
        String name = requireUsableName(renameTo.trim());
        repository.liveByName(name)
                .filter(other -> !other.id().equals(emote.id()))
                .ifPresent(other -> {
                    throw new EmoteException.Duplicate(name + " is already taken.");
                });
        return name;
    }

    private String publish(String fileName, byte[] data, String message) {
        try {
            return github.putSource(fileName, data, message);
        } catch (RuntimeException e) {
            log.warn("Commit of {} failed", fileName, e);
            throw new EmoteException.PublishFailed("Commit failed: " + rootMessage(e));
        }
    }

    private void unpublish(Emote emote) {
        String fileName = emote.sourceFileName();
        String sha = Optional.ofNullable(emote.githubSha())
                .or(() -> github.shaFor(fileName))
                .orElseThrow(() -> new EmoteException.NotFound(
                        "That image isn't in the repo any more, so there is nothing to delete."));
        try {
            github.deleteSource(fileName, sha, "Remove " + emote.name());
        } catch (RuntimeException e) {
            throw new EmoteException.PublishFailed("Delete failed: " + rootMessage(e));
        }
    }

    private Emote require(String id) {
        return repository.byId(id)
                .orElseThrow(() -> new EmoteException.NotFound("No such emote."));
    }

    private static String trimmed(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        return reason.substring(0, Math.min(reason.length(), 200));
    }

    private static String rootMessage(RuntimeException e) {
        Throwable cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage();
    }
}
