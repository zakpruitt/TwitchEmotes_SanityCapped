package com.zakpruitt.sanitycapped.emote.service;

import com.zakpruitt.sanitycapped.emote.model.Emote;
import com.zakpruitt.sanitycapped.emote.exception.EmoteException;
import com.zakpruitt.sanitycapped.emote.repository.EmoteRepository;
import com.zakpruitt.sanitycapped.image.ImageStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/** The admin's decisions: approve, reject, remove. */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmoteReviewService {

    private static final int MAX_REASON_LENGTH = 200;

    private final EmoteRepository emotes;
    private final ImageStore images;
    private final NamePolicy names;
    private final DuplicateDetector duplicates;
    private final PackPublisher publisher;
    private final Clock clock;

    /** Committing the image is the publish step: the push is what builds the release. */
    @Transactional
    public Emote approve(String id, String renameTo) {
        Emote emote = require(id);
        if (emote.isApproved()) {
            return emote;
        }
        publisher.requireEnabled();

        String name = finalName(emote, renameTo);
        byte[] data = images.get(emote.fileName()).orElseThrow(() ->
                new EmoteException.MissingImage("The uploaded image is missing from storage."));
        String githubSha = publisher.publish(emote, name, data);

        emote.approve(name, githubSha, clock.instant());
        log.info("Approved {}", name);
        return emote;
    }

    @Transactional
    public Emote reject(String id, String reason) {
        Emote emote = require(id);
        if (emote.isApproved()) {
            throw new EmoteException.Duplicate("Already in the pack. Remove it instead.");
        }

        emote.reject(truncatedReason(reason), clock.instant());
        log.info("Rejected {}", emote.getName());
        return emote;
    }

    /** Deleting the source image makes the next build drop its texture too. */
    @Transactional
    public void remove(String id) {
        Emote emote = require(id);
        if (emote.isApproved()) {
            publisher.unpublish(emote);
        }

        images.delete(emote.fileName());
        emotes.delete(emote);
        log.info("Removed {}", emote.getName());
    }

    private Emote require(String id) {
        return emotes.findById(id)
                .orElseThrow(() -> new EmoteException.NotFound("No such emote."));
    }

    private String finalName(Emote emote, String renameTo) {
        if (renameTo == null || renameTo.isBlank()) {
            return emote.getName();
        }

        String name = names.requireUsable(renameTo.trim());
        duplicates.refuseNameTakenByOther(name, emote);
        return name;
    }

    private static String truncatedReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        return reason.substring(0, Math.min(reason.length(), MAX_REASON_LENGTH));
    }
}
