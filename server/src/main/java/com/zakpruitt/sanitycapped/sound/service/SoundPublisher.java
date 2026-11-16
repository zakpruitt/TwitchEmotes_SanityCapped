package com.zakpruitt.sanitycapped.sound.service;

import com.zakpruitt.sanitycapped.emote.exception.EmoteException;
import com.zakpruitt.sanitycapped.emote.service.PackPublisher;
import com.zakpruitt.sanitycapped.github.GitHubClient;
import com.zakpruitt.sanitycapped.sound.model.Sound;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Adds and removes sounds in the repo's sound pack folder, in the language of sounds. */
@Component
@Slf4j
@RequiredArgsConstructor
public class SoundPublisher {

    private final GitHubClient github;
    private final PackPublisher pack;

    public void requireEnabled() {
        pack.requireEnabled();
    }

    /** @return the committed blob's sha, which a later removal has to quote */
    public String publish(Sound sound, String name, byte[] data) {
        String fileName = name + ".mp3";
        String message = "Add sound %s (uploaded by %s)".formatted(name, sound.getUploader());

        try {
            return github.putSound(fileName, data, message);
        } catch (RuntimeException e) {
            log.warn("Commit of {} failed", fileName, e);
            throw new EmoteException.PublishFailed("Commit failed: " + PackPublisher.rootMessage(e));
        }
    }

    public void unpublish(Sound sound) {
        String fileName = sound.sourceFileName();
        String sha = committedSha(sound);

        try {
            github.deleteSound(fileName, sha, "Remove sound " + sound.getName());
        } catch (RuntimeException e) {
            log.warn("Delete of {} failed", fileName, e);
            throw new EmoteException.PublishFailed("Delete failed: " + PackPublisher.rootMessage(e));
        }
    }

    private String committedSha(Sound sound) {
        return Optional.ofNullable(sound.getGithubSha())
                .or(() -> github.soundShaFor(sound.sourceFileName()))
                .orElseThrow(() -> new EmoteException.NotFound(
                        "That sound isn't in the repo any more, so there is nothing to delete."));
    }
}
