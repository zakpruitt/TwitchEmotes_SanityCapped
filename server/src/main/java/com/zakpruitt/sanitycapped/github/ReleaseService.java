package com.zakpruitt.sanitycapped.github;

import com.zakpruitt.sanitycapped.config.AppProperties;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ReleaseService {

    private final GitHubClient github;
    private final AppProperties props;

    ReleaseService(GitHubClient github, AppProperties props) {
        this.github = github;
        this.props = props;
    }

    public Optional<GitHubClient.Release> latest() {
        return github.latestRelease();
    }

    public String installUrl() {
        return props.repoUrl();
    }
}
