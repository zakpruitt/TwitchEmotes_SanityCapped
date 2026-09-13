package gg.sanitycapped.emotes.github;

import java.util.Optional;

import org.springframework.stereotype.Service;

import gg.sanitycapped.emotes.config.AppProperties;

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
