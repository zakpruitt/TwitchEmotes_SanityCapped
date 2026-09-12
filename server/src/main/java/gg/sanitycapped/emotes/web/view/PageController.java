package gg.sanitycapped.emotes.web.view;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import gg.sanitycapped.emotes.config.AppProperties;
import gg.sanitycapped.emotes.emote.EmoteService;
import gg.sanitycapped.emotes.github.GitHubClient;
import gg.sanitycapped.emotes.web.EmoteView;

/**
 * The three pages. Browse is rendered server-side from the database, so it is
 * useful with no JavaScript at all; upload and admin are forms that talk to the
 * API from there.
 */
@Controller
class PageController {

    private final EmoteService emotes;
    private final GitHubClient github;
    private final AppProperties props;

    PageController(EmoteService emotes, GitHubClient github, AppProperties props) {
        this.emotes = emotes;
        this.github = github;
        this.props = props;
    }

    /** Every page shows the install URL, so the layout always has it. */
    @ModelAttribute("repoUrl")
    String repoUrl() {
        return props.repoUrl();
    }

    @GetMapping("/")
    String browse(Model model) {
        model.addAttribute("emotes", emotes.approved().stream().map(EmoteView::of).toList());
        model.addAttribute("release", github.latestRelease().orElse(null));
        return "browse";
    }

    @GetMapping("/upload")
    String upload(Model model) {
        model.addAttribute("maxMegabytes", props.maxUploadMegabytes());
        return "upload";
    }

    @GetMapping("/admin")
    String admin(Model model) {
        model.addAttribute("publishingEnabled", props.canPublish());
        return "admin";
    }
}
