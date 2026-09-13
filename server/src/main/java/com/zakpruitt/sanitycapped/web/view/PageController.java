package com.zakpruitt.sanitycapped.web.view;

import com.zakpruitt.sanitycapped.config.AppProperties;
import com.zakpruitt.sanitycapped.emote.service.EmoteService;
import com.zakpruitt.sanitycapped.github.ReleaseService;
import com.zakpruitt.sanitycapped.web.dto.response.EmoteListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
@RequiredArgsConstructor
class PageController {

    private final EmoteService emotes;
    private final ReleaseService releases;
    private final AppProperties props;


    @ModelAttribute("repoUrl")
    String repoUrl() {
        return releases.installUrl();
    }

    @GetMapping("/")
    String browse(Model model) {
        model.addAttribute("emotes", EmoteListResponse.from(emotes.approved()).emotes());
        model.addAttribute("release", releases.latest().orElse(null));
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
