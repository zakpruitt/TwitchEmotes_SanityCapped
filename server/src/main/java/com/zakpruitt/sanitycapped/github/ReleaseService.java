package com.zakpruitt.sanitycapped.github;

import com.zakpruitt.sanitycapped.github.dto.Release;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReleaseService {

    private final GitHubClient github;

    /** Empty when GitHub is unreachable or nothing has been released yet. */
    public Optional<Release> latest() {
        return github.latestRelease();
    }
}
