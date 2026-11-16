package com.zakpruitt.sanitycapped.sound.service;

import com.zakpruitt.sanitycapped.emote.dto.DuplicateCheck;
import com.zakpruitt.sanitycapped.emote.exception.EmoteException;
import com.zakpruitt.sanitycapped.emote.model.EmoteStatus;
import com.zakpruitt.sanitycapped.naming.Naming;
import com.zakpruitt.sanitycapped.sound.model.Sound;
import com.zakpruitt.sanitycapped.sound.repository.SoundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * The two duplicate rules for sounds: the same command and the same bytes.
 * Rejected sounds count for neither.
 */
@Service
@RequiredArgsConstructor
public class SoundDuplicateDetector {

    private final SoundRepository sounds;

    /** The upload form's live pre-check; reports clashes rather than throwing. */
    @Transactional(readOnly = true)
    public DuplicateCheck check(String typedName, String sha256) {
        String name = Naming.soundName(typedName);

        EmoteStatus nameTaken = name.isEmpty() ? null
                : liveByName(name).map(Sound::getStatus).orElse(null);
        String exact = sha256 == null || sha256.isBlank() ? null
                : liveBySha256(sha256).map(Sound::getName).orElse(null);

        return new DuplicateCheck(name, nameTaken, exact);
    }

    public void refuse(String name, String sha256) {
        liveByName(name).ifPresent(clash -> {
            throw new EmoteException.Duplicate(clash.isApproved()
                    ? clash.getName() + " already exists in the pack."
                    : clash.getName() + " is already waiting for approval.");
        });

        liveBySha256(sha256).ifPresent(dupe -> {
            throw new EmoteException.Duplicate(
                    "That exact sound is already here as " + dupe.getName() + ".");
        });
    }

    /** For renames on approval, where the sound itself holds its current name. */
    public void refuseNameTakenByOther(String name, Sound sound) {
        liveByName(name)
                .filter(other -> !other.getId().equals(sound.getId()))
                .ifPresent(other -> {
                    throw new EmoteException.Duplicate(name + " is already taken.");
                });
    }

    public boolean isNameLive(String name) {
        return liveByName(name).isPresent();
    }

    private Optional<Sound> liveByName(String name) {
        return sounds.findByNameIgnoreCaseAndStatusIn(name, EmoteStatus.LIVE);
    }

    private Optional<Sound> liveBySha256(String sha256) {
        return sounds.findBySha256AndStatusIn(sha256, EmoteStatus.LIVE);
    }
}
