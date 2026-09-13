package com.zakpruitt.sanitycapped.emote.service;

import com.zakpruitt.sanitycapped.config.AppProperties;
import com.zakpruitt.sanitycapped.emote.model.Emote;
import com.zakpruitt.sanitycapped.emote.model.EmoteStatus;
import com.zakpruitt.sanitycapped.emote.exception.EmoteException;
import com.zakpruitt.sanitycapped.emote.dto.DuplicateCheck;
import com.zakpruitt.sanitycapped.emote.repository.EmoteRepository;
import com.zakpruitt.sanitycapped.image.Hashes;
import com.zakpruitt.sanitycapped.naming.Naming;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * The three duplicate rules: the same trigger word, the same bytes, and (only
 * as a warning) an image that looks the same. Rejected emotes count for none.
 */
@Service
@RequiredArgsConstructor
public class DuplicateDetector {

    private final EmoteRepository emotes;
    private final AppProperties props;

    /** The upload form's live pre-check; reports clashes rather than throwing. */
    @Transactional(readOnly = true)
    public DuplicateCheck check(String typedName, String sha256) {
        String name = Naming.triggerWord(typedName);

        EmoteStatus nameTaken = name.isEmpty() ? null
                : liveByName(name).map(Emote::getStatus).orElse(null);
        String exact = sha256 == null || sha256.isBlank() ? null
                : liveBySha256(sha256).map(Emote::getName).orElse(null);

        return new DuplicateCheck(name, nameTaken, exact);
    }

    public void refuse(String name, String sha256) {
        liveByName(name).ifPresent(clash -> {
            throw new EmoteException.Duplicate(clash.isApproved()
                    ? name + " already exists in the pack."
                    : name + " is already waiting for approval.");
        });

        liveBySha256(sha256).ifPresent(dupe -> {
            throw new EmoteException.Duplicate(
                    "That exact image is already here as " + dupe.getName() + ".");
        });
    }

    /** For renames on approval, where the emote itself holds its current name. */
    public void refuseNameTakenByOther(String name, Emote emote) {
        liveByName(name)
                .filter(other -> !other.getId().equals(emote.getId()))
                .ifPresent(other -> {
                    throw new EmoteException.Duplicate(name + " is already taken.");
                });
    }

    /** Warned about rather than refused: a resized variant is sometimes the point. */
    public String nearDuplicateWarning(String dhash) {
        return emotes.findByStatusIn(EmoteStatus.LIVE).stream()
                .filter(other -> Hashes.hamming(dhash, other.getDhash()) <= props.similarThreshold())
                .findFirst()
                .map(near -> "Looks a lot like " + near.getName())
                .orElse(null);
    }

    public boolean isNameLive(String name) {
        return liveByName(name).isPresent();
    }

    private Optional<Emote> liveByName(String name) {
        return emotes.findByNameAndStatusIn(name, EmoteStatus.LIVE);
    }

    private Optional<Emote> liveBySha256(String sha256) {
        return emotes.findBySha256AndStatusIn(sha256, EmoteStatus.LIVE);
    }
}
