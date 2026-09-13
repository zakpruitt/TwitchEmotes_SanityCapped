package com.zakpruitt.sanitycapped.emote;

import com.zakpruitt.sanitycapped.github.GitHubClient;
import com.zakpruitt.sanitycapped.image.Hashes;
import com.zakpruitt.sanitycapped.image.ImageInfo;
import com.zakpruitt.sanitycapped.image.ImageInspector;
import com.zakpruitt.sanitycapped.naming.Naming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Adopts whatever is already in the repo's tools/source/. The repo is the
 * archive and this machine's disk a cache, so a fresh volume comes up holding
 * the whole pack and emotes added by hand turn up on the site too.
 */
@Component
public class RepoSync implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RepoSync.class);

    private final EmoteRepository emotes;
    private final ImageStore images;
    private final ImageInspector inspector;
    private final GitHubClient github;
    private final Clock clock;

    RepoSync(EmoteRepository emotes, ImageStore images, ImageInspector inspector,
             GitHubClient github, Clock clock) {
        this.emotes = emotes;
        this.images = images;
        this.inspector = inspector;
        this.github = github;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            sync();
        } catch (RuntimeException e) {
            // The queue works offline, so this must never stop the app.
            log.warn("Could not sync from the repo at startup: {}", e.getMessage());
        }
    }

    @Transactional
    public int sync() {
        int adopted = 0;
        for (GitHubClient.SourceFile file : github.listSource()) {
            if (adopt(file)) {
                adopted++;
            }
        }
        if (adopted > 0) {
            log.info("Adopted {} emote(s) already in the repo", adopted);
        }
        return adopted;
    }

    private boolean adopt(GitHubClient.SourceFile file) {
        String name = Naming.triggerWord(file.stem());
        if (emotes.findByNameAndStatusIn(name, EmoteStatus.LIVE).isPresent()) {
            return false;
        }
        byte[] data = github.download(file.downloadUrl());
        ImageInfo info = inspector.inspect(data).orElse(null);
        if (info == null) {
            log.warn("Skipping {}: not an image we can read", file.name());
            return false;
        }
        Emote emote = Emote.adopted(name, data, info, Hashes.sha256(data), file.sha(), clock.instant());
        images.put(emote.fileName(), data);
        emotes.save(emote);
        return true;
    }
}
