package com.zakpruitt.sanitycapped.emote.service;

import com.zakpruitt.sanitycapped.emote.model.Emote;
import com.zakpruitt.sanitycapped.emote.repository.EmoteRepository;
import com.zakpruitt.sanitycapped.github.GitHubClient;
import com.zakpruitt.sanitycapped.github.dto.SourceFile;
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
import java.util.Optional;

/**
 * Adopts whatever is already in the repo's tools/source/. The repo is the
 * archive and this machine's disk a cache, so a fresh volume comes up holding
 * the whole pack and emotes added by hand turn up on the site too.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RepoSyncService {

    private final EmoteRepository emotes;
    private final ImageStore images;
    private final ImageInspector inspector;
    private final DuplicateDetector duplicates;
    private final GitHubClient github;
    private final Clock clock;

    /** @return how many emotes were new to this site */
    @Transactional
    public int sync() {
        int adopted = (int) github.listSource().stream()
                .filter(this::adopt)
                .count();

        if (adopted > 0) {
            log.info("Adopted {} emote(s) already in the repo", adopted);
        }
        return adopted;
    }

    private boolean adopt(SourceFile file) {
        String name = Naming.triggerWord(Naming.stem(file.name()));
        if (duplicates.isNameLive(name)) {
            return false;
        }

        byte[] data = github.download(file.downloadUrl());
        Optional<ImageInfo> info = inspector.inspect(data);
        if (info.isEmpty()) {
            log.warn("Skipping {}: not an image we can read", file.name());
            return false;
        }

        Emote emote = Emote.adopted(name, data, info.get(), Hashes.sha256(data), file.sha(), clock.instant());
        images.put(emote.fileName(), data);
        emotes.save(emote);
        return true;
    }
}
