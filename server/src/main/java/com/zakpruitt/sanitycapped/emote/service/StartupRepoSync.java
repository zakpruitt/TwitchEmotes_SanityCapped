package com.zakpruitt.sanitycapped.emote.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Runs the repo sync once on boot. Kept apart from RepoSyncService so the call
 * goes through its transactional proxy.
 */
@Component
@Slf4j
@RequiredArgsConstructor
class StartupRepoSync implements ApplicationRunner {

    private final RepoSyncService repoSync;

    @Override
    public void run(ApplicationArguments args) {
        try {
            repoSync.sync();
        } catch (RuntimeException e) {
            // The queue works offline, so this must never stop the app.
            log.warn("Could not sync from the repo at startup: {}", e.getMessage());
        }
    }
}
