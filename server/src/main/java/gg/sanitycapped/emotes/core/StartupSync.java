package gg.sanitycapped.emotes.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Adopts whatever is already in the repo's tools/source/ on boot, so a fresh
 * volume (or a first deploy) comes up with the whole pack instead of an empty
 * browse page. Failing here must never stop the app: uploads still work offline.
 */
@Component
class StartupSync implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupSync.class);

    private final EmoteService emotes;

    StartupSync(EmoteService emotes) {
        this.emotes = emotes;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            emotes.syncFromRepo();
        } catch (RuntimeException e) {
            log.warn("Could not sync from the repo at startup: {}", e.getMessage());
        }
    }
}
