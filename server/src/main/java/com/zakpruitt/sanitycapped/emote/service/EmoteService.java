package com.zakpruitt.sanitycapped.emote.service;

import com.zakpruitt.sanitycapped.config.AppProperties;
import com.zakpruitt.sanitycapped.emote.Emote;
import com.zakpruitt.sanitycapped.emote.EmoteStatus;
import com.zakpruitt.sanitycapped.emote.UploadLog;
import com.zakpruitt.sanitycapped.emote.exception.EmoteException;
import com.zakpruitt.sanitycapped.emote.model.DuplicateCheck;
import com.zakpruitt.sanitycapped.emote.model.EmoteUpload;
import com.zakpruitt.sanitycapped.emote.repository.EmoteRepository;
import com.zakpruitt.sanitycapped.emote.repository.UploadLogRepository;
import com.zakpruitt.sanitycapped.github.GitHubClient;
import com.zakpruitt.sanitycapped.image.Hashes;
import com.zakpruitt.sanitycapped.image.ImageInfo;
import com.zakpruitt.sanitycapped.image.ImageInspector;
import com.zakpruitt.sanitycapped.naming.Naming;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmoteService {

    private static final int MAX_NAME_LENGTH = 40;
    private static final Duration RATE_WINDOW = Duration.ofHours(1);

    private final AppProperties props;
    private final EmoteRepository emotes;
    private final UploadLogRepository uploadLog;
    private final ImageStore images;
    private final ImageInspector inspector;
    private final GitHubClient github;
    private final Clock clock;


    @Transactional(readOnly = true)
    public List<Emote> approved() {
        return emotes.findByStatusOrderByCreatedAtDesc(EmoteStatus.APPROVED);
    }

    @Transactional(readOnly = true)
    public List<Emote> pending() {
        return emotes.findByStatusOrderByCreatedAtAsc(EmoteStatus.PENDING);
    }

    public Optional<byte[]> image(String fileName) {
        return images.get(fileName);
    }

    @Transactional(readOnly = true)
    public DuplicateCheck check(String typedName, String sha256) {
        String name = Naming.triggerWord(typedName);
        return new DuplicateCheck(
                name,
                name.isEmpty() ? null : liveByName(name).map(Emote::getStatus).orElse(null),
                sha256 == null || sha256.isBlank() ? null
                        : emotes.findBySha256AndStatusIn(sha256, EmoteStatus.LIVE)
                                .map(Emote::getName).orElse(null));
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
        emotes.save(emote);
        uploadLog.save(new UploadLog(upload.ip(), now));

        log.info("Queued {} from {} ({} bytes{})", emote.getName(), emote.getUploader(),
                emote.getBytes(), emote.isAnimated() ? ", animated" : "");
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

        String name = renameTo == null || renameTo.isBlank()
                ? emote.getName() : rename(emote, renameTo);
        byte[] data = images.get(emote.fileName()).orElseThrow(() -> new EmoteException.MissingImage(
                "The uploaded image is missing from storage."));

        String githubSha = publish(name + "." + emote.getExt(), data,
                "Add %s (uploaded by %s)".formatted(name, emote.getUploader()));

        log.info("Approved {}, committed as {}", emote.getName(), name);
        emote.approve(name, githubSha, clock.instant());
        return emote;
    }

    @Transactional
    public String reject(String id, String reason) {
        Emote emote = require(id);
        if (emote.isApproved()) {
            throw new EmoteException.Duplicate("Already in the pack. Remove it instead.");
        }
        emote.reject(trimmed(reason), clock.instant());
        log.info("Rejected {}", emote.getName());
        return emote.getName();
    }

    /** Deleting the source image makes the next build drop its texture too. */
    @Transactional
    public void remove(String id) {
        Emote emote = require(id);
        if (emote.isApproved()) {
            unpublish(emote);
        }
        images.delete(emote.fileName());
        emotes.delete(emote);
        log.info("Removed {}", emote.getName());
    }

    private Optional<Emote> liveByName(String name) {
        return emotes.findByNameAndStatusIn(name, EmoteStatus.LIVE);
    }

    private static String namedBy(EmoteUpload upload) {
        if (upload.requestedName() != null && !upload.requestedName().isBlank()) {
            return upload.requestedName();
        }
        String fileName = upload.originalFileName();
        return fileName == null ? "" : fileName.replaceFirst("\\.[^.]+$", "");
    }

    private void checkRateLimit(String ip, Instant now) {
        if (uploadLog.countByIpAndAtAfter(ip, now.minus(RATE_WINDOW)) >= props.uploadsPerHour()) {
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
        liveByName(name).ifPresent(clash -> {
            throw new EmoteException.Duplicate(clash.isApproved()
                    ? name + " already exists in the pack."
                    : name + " is already waiting for approval.");
        });
        emotes.findBySha256AndStatusIn(sha256, EmoteStatus.LIVE).ifPresent(dupe -> {
            throw new EmoteException.Duplicate(
                    "That exact image is already here as " + dupe.getName() + ".");
        });
    }

    /** Warned about rather than refused: a resized variant is sometimes the point. */
    private String nearDuplicateWarning(String dhash) {
        return emotes.findByStatusIn(EmoteStatus.LIVE).stream()
                .filter(other -> Hashes.hamming(dhash, other.getDhash()) <= props.similarThreshold())
                .findFirst()
                .map(near -> "Looks a lot like " + near.getName())
                .orElse(null);
    }

    private String rename(Emote emote, String renameTo) {
        String name = requireUsableName(renameTo.trim());
        liveByName(name)
                .filter(other -> !other.getId().equals(emote.getId()))
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
        String sha = Optional.ofNullable(emote.getGithubSha())
                .or(() -> github.shaFor(fileName))
                .orElseThrow(() -> new EmoteException.NotFound(
                        "That image isn't in the repo any more, so there is nothing to delete."));
        try {
            github.deleteSource(fileName, sha, "Remove " + emote.getName());
        } catch (RuntimeException e) {
            throw new EmoteException.PublishFailed("Delete failed: " + rootMessage(e));
        }
    }

    private Emote require(String id) {
        return emotes.findById(id)
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
