package com.zakpruitt.sanitycapped.emote.service;

import com.zakpruitt.sanitycapped.config.AppProperties;
import com.zakpruitt.sanitycapped.emote.model.Emote;
import com.zakpruitt.sanitycapped.emote.exception.EmoteException;
import com.zakpruitt.sanitycapped.emote.dto.EmoteUpload;
import com.zakpruitt.sanitycapped.emote.repository.EmoteRepository;
import com.zakpruitt.sanitycapped.image.Hashes;
import com.zakpruitt.sanitycapped.image.dto.ImageInfo;
import com.zakpruitt.sanitycapped.image.ImageInspector;
import com.zakpruitt.sanitycapped.image.ImageStore;
import com.zakpruitt.sanitycapped.naming.Naming;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/** Turns a guild member's upload into a pending emote, or explains why not. */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmoteUploadService {

    private final AppProperties props;
    private final EmoteRepository emotes;
    private final ImageStore images;
    private final ImageInspector inspector;
    private final NamePolicy names;
    private final DuplicateDetector duplicates;
    private final UploadRateLimiter rateLimiter;
    private final Clock clock;

    @Transactional
    public Emote upload(EmoteUpload upload) {
        Instant now = clock.instant();
        rateLimiter.requireAllowance(upload.ip(), now);
        requireUploader(upload);
        requireWithinSizeLimit(upload);

        ImageInfo info = inspect(upload.data());
        String name = names.requireUsable(requestedName(upload));
        String sha256 = Hashes.sha256(upload.data());
        duplicates.refuse(name, sha256);

        Emote emote = Emote.pending(name, upload.data(), info, upload.uploader().trim(), sha256,
                duplicates.nearDuplicateWarning(info.dhash()), now);
        images.put(emote.fileName(), upload.data());
        emotes.save(emote);
        rateLimiter.record(upload.ip(), now);

        log.info("Queued {} from {} ({} bytes{})", emote.getName(), emote.getUploader(),
                emote.getBytes(), emote.isAnimated() ? ", animated" : "");
        return emote;
    }

    private static void requireUploader(EmoteUpload upload) {
        if (upload.uploader() == null || upload.uploader().isBlank()) {
            throw new EmoteException.Invalid("Add your name so we know who to thank.");
        }
    }

    private void requireWithinSizeLimit(EmoteUpload upload) {
        if (upload.data().length > props.maxUploadBytes()) {
            throw new EmoteException.Invalid(
                    "That image is over %d MB. Trim it down first.".formatted(props.maxUploadMegabytes()));
        }
    }

    private ImageInfo inspect(byte[] data) {
        return inspector.inspect(data)
                .orElseThrow(() -> new EmoteException.Invalid("That isn't a GIF, PNG, WebP or JPEG."));
    }

    /** The typed name wins; otherwise fall back to the file's name. */
    private static String requestedName(EmoteUpload upload) {
        if (upload.requestedName() != null && !upload.requestedName().isBlank()) {
            return upload.requestedName();
        }
        return Naming.stem(upload.originalFileName());
    }
}
