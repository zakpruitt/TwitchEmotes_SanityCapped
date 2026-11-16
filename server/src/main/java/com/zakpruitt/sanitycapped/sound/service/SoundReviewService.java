package com.zakpruitt.sanitycapped.sound.service;

import com.zakpruitt.sanitycapped.audio.SoundStore;
import com.zakpruitt.sanitycapped.emote.exception.EmoteException;
import com.zakpruitt.sanitycapped.emote.service.NamePolicy;
import com.zakpruitt.sanitycapped.sound.model.Sound;
import com.zakpruitt.sanitycapped.sound.repository.SoundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/** The admin's decisions on sounds: approve, reject, remove. */
@Service
@Slf4j
@RequiredArgsConstructor
public class SoundReviewService {

    private static final int MAX_REASON_LENGTH = 200;

    private final SoundRepository sounds;
    private final SoundStore files;
    private final NamePolicy names;
    private final SoundDuplicateDetector duplicates;
    private final SoundPublisher publisher;
    private final Clock clock;

    /** Committing the MP3 is the publish step: the push is what builds the release. */
    @Transactional
    public Sound approve(String id, String renameTo) {
        Sound sound = require(id);
        if (sound.isApproved()) {
            return sound;
        }
        publisher.requireEnabled();

        String name = finalName(sound, renameTo);
        byte[] data = files.get(sound.fileName()).orElseThrow(() ->
                new EmoteException.MissingImage("The uploaded sound is missing from storage."));
        String githubSha = publisher.publish(sound, name, data);

        sound.approve(name, githubSha, clock.instant());
        log.info("Approved sound {}", name);
        return sound;
    }

    @Transactional
    public Sound reject(String id, String reason) {
        Sound sound = require(id);
        if (sound.isApproved()) {
            throw new EmoteException.Duplicate("Already in the pack. Remove it instead.");
        }

        sound.reject(truncatedReason(reason), clock.instant());
        log.info("Rejected sound {}", sound.getName());
        return sound;
    }

    /** Deleting the MP3 from the repo makes the next build drop it from the pack. */
    @Transactional
    public void remove(String id) {
        Sound sound = require(id);
        if (sound.isApproved()) {
            publisher.unpublish(sound);
        }

        files.delete(sound.fileName());
        sounds.delete(sound);
        log.info("Removed sound {}", sound.getName());
    }

    private Sound require(String id) {
        return sounds.findById(id)
                .orElseThrow(() -> new EmoteException.NotFound("No such sound."));
    }

    private String finalName(Sound sound, String renameTo) {
        if (renameTo == null || renameTo.isBlank()) {
            return sound.getName();
        }

        String name = names.requireUsableSound(renameTo.trim());
        duplicates.refuseNameTakenByOther(name, sound);
        return name;
    }

    private static String truncatedReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        return reason.substring(0, Math.min(reason.length(), MAX_REASON_LENGTH));
    }
}
