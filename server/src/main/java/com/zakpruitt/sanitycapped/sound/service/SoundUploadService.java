package com.zakpruitt.sanitycapped.sound.service;

import com.zakpruitt.sanitycapped.audio.Mp3Inspector;
import com.zakpruitt.sanitycapped.audio.SoundStore;
import com.zakpruitt.sanitycapped.audio.dto.AudioInfo;
import com.zakpruitt.sanitycapped.config.AppProperties;
import com.zakpruitt.sanitycapped.emote.exception.EmoteException;
import com.zakpruitt.sanitycapped.emote.service.NamePolicy;
import com.zakpruitt.sanitycapped.emote.service.UploadRateLimiter;
import com.zakpruitt.sanitycapped.image.Hashes;
import com.zakpruitt.sanitycapped.naming.Naming;
import com.zakpruitt.sanitycapped.sound.dto.SoundUpload;
import com.zakpruitt.sanitycapped.sound.model.Sound;
import com.zakpruitt.sanitycapped.sound.repository.SoundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/** Turns a guild member's upload into a pending sound, or explains why not. */
@Service
@Slf4j
@RequiredArgsConstructor
public class SoundUploadService {

    private final AppProperties props;
    private final SoundRepository sounds;
    private final SoundStore files;
    private final Mp3Inspector inspector;
    private final NamePolicy names;
    private final SoundDuplicateDetector duplicates;
    private final UploadRateLimiter rateLimiter;
    private final Clock clock;

    @Transactional
    public Sound upload(SoundUpload upload) {
        Instant now = clock.instant();
        rateLimiter.requireAllowance(upload.ip(), now);
        requireUploader(upload);
        requireWithinSizeLimit(upload);

        AudioInfo info = inspect(upload.data());
        requireShortEnough(info);
        String name = names.requireUsableSound(requestedName(upload));
        String sha256 = Hashes.sha256(upload.data());
        duplicates.refuse(name, sha256);

        Sound sound = Sound.pending(name, upload.data(), info, upload.uploader().trim(), sha256, now);
        files.put(sound.fileName(), upload.data());
        sounds.save(sound);
        rateLimiter.record(upload.ip(), now);

        log.info("Queued sound {} from {} ({} bytes, {}s)", sound.getName(), sound.getUploader(),
                sound.getBytes(), sound.seconds());
        return sound;
    }

    private static void requireUploader(SoundUpload upload) {
        if (upload.uploader() == null || upload.uploader().isBlank()) {
            throw new EmoteException.Invalid("Add your name so we know who to thank.");
        }
    }

    private void requireWithinSizeLimit(SoundUpload upload) {
        if (upload.data().length > props.maxUploadBytes()) {
            throw new EmoteException.Invalid(
                    "That sound is over %d MB. Trim it down first.".formatted(props.maxUploadMegabytes()));
        }
    }

    private AudioInfo inspect(byte[] data) {
        return inspector.inspect(data)
                .orElseThrow(() -> new EmoteException.Invalid("That isn't an MP3."));
    }

    private void requireShortEnough(AudioInfo info) {
        if (info.seconds() > props.maxSoundSeconds()) {
            throw new EmoteException.Invalid("That sound is %d seconds long. Keep it under %d."
                    .formatted(Math.round(info.seconds()), props.maxSoundSeconds()));
        }
    }

    /** The typed name wins; otherwise fall back to the file's name. */
    private static String requestedName(SoundUpload upload) {
        if (upload.requestedName() != null && !upload.requestedName().isBlank()) {
            return upload.requestedName();
        }
        return Naming.stem(upload.originalFileName());
    }
}
