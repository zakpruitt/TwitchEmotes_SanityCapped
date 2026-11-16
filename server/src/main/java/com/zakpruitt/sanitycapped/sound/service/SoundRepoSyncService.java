package com.zakpruitt.sanitycapped.sound.service;

import com.zakpruitt.sanitycapped.audio.Mp3Inspector;
import com.zakpruitt.sanitycapped.audio.SoundStore;
import com.zakpruitt.sanitycapped.audio.dto.AudioInfo;
import com.zakpruitt.sanitycapped.github.GitHubClient;
import com.zakpruitt.sanitycapped.github.dto.SourceFile;
import com.zakpruitt.sanitycapped.image.Hashes;
import com.zakpruitt.sanitycapped.naming.Naming;
import com.zakpruitt.sanitycapped.sound.model.Sound;
import com.zakpruitt.sanitycapped.sound.repository.SoundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

/**
 * Adopts whatever is already in the repo's sound pack folder, the same way
 * RepoSyncService does for emotes: the repo is the archive, this disk a cache.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SoundRepoSyncService {

    private final SoundRepository sounds;
    private final SoundStore files;
    private final Mp3Inspector inspector;
    private final SoundDuplicateDetector duplicates;
    private final GitHubClient github;
    private final Clock clock;

    /** @return how many sounds were new to this site */
    @Transactional
    public int sync() {
        int adopted = (int) github.listSounds().stream()
                .filter(file -> file.name().toLowerCase().endsWith(".mp3"))
                .filter(this::adopt)
                .count();

        if (adopted > 0) {
            log.info("Adopted {} sound(s) already in the repo", adopted);
        }
        return adopted;
    }

    private boolean adopt(SourceFile file) {
        String name = Naming.soundName(Naming.stem(file.name()));
        if (name.isEmpty() || duplicates.isNameLive(name)) {
            return false;
        }

        byte[] data = github.download(file.downloadUrl());
        Optional<AudioInfo> info = inspector.inspect(data);
        if (info.isEmpty()) {
            log.warn("Skipping {}: not an MP3 we can read", file.name());
            return false;
        }

        Sound sound = Sound.adopted(name, data, info.get(), Hashes.sha256(data), file.sha(), clock.instant());
        files.put(sound.fileName(), data);
        sounds.save(sound);
        return true;
    }
}
