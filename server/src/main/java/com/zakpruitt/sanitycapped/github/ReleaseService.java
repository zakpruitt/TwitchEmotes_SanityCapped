package com.zakpruitt.sanitycapped.github;

import com.zakpruitt.sanitycapped.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReleaseService {

    private final GitHubClient github;
    private final AppProperties props;


    public Optional<GitHubClient.Release> latest() {
        return github.latestRelease();
    }

    public String installUrl() {
        return props.repoUrl();
    }
}
