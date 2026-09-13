package com.zakpruitt.sanitycapped.emote.service;

import com.zakpruitt.sanitycapped.config.AppProperties;
import com.zakpruitt.sanitycapped.emote.model.Emote;
import com.zakpruitt.sanitycapped.emote.exception.EmoteException;
import com.zakpruitt.sanitycapped.github.GitHubClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Adds and removes emotes in the repo's tools/source/, in the language of emotes. */
@Component
@Slf4j
@RequiredArgsConstructor
public class PackPublisher {

    private final GitHubClient github;
    private final AppProperties props;

    public void requireEnabled() {
        if (!props.canPublish()) {
            throw new EmoteException.PublishingDisabled(
                    "No GitHub token configured, so nothing can be published.");
        }
    }

    /** @return the committed blob's sha, which a later removal has to quote */
    public String publish(Emote emote, String name, byte[] data) {
        String fileName = name + "." + emote.getExt();
        String message = "Add %s (uploaded by %s)".formatted(name, emote.getUploader());

        try {
            return github.putSource(fileName, data, message);
        } catch (RuntimeException e) {
            log.warn("Commit of {} failed", fileName, e);
            throw new EmoteException.PublishFailed("Commit failed: " + rootMessage(e));
        }
    }

    public void unpublish(Emote emote) {
        String fileName = emote.sourceFileName();
        String sha = committedSha(emote);

        try {
            github.deleteSource(fileName, sha, "Remove " + emote.getName());
        } catch (RuntimeException e) {
            log.warn("Delete of {} failed", fileName, e);
            throw new EmoteException.PublishFailed("Delete failed: " + rootMessage(e));
        }
    }

    /** Emotes adopted before we tracked shas have to ask GitHub for theirs. */
    private String committedSha(Emote emote) {
        return Optional.ofNullable(emote.getGithubSha())
                .or(() -> github.shaFor(emote.sourceFileName()))
                .orElseThrow(() -> new EmoteException.NotFound(
                        "That image isn't in the repo any more, so there is nothing to delete."));
    }

    private static String rootMessage(Throwable e) {
        Throwable cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage();
    }
}
