package gg.sanitycapped.emotes.web.api;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import gg.sanitycapped.emotes.emote.DuplicateCheck;
import gg.sanitycapped.emotes.emote.Emote;
import gg.sanitycapped.emotes.emote.EmoteService;
import gg.sanitycapped.emotes.web.EmoteView;
import gg.sanitycapped.emotes.web.Passcodes;

/** What the upload form and the browse page call. */
@RestController
class EmoteApiController {

    private final EmoteService emotes;
    private final Passcodes passcodes;

    EmoteApiController(EmoteService emotes, Passcodes passcodes) {
        this.emotes = emotes;
        this.passcodes = passcodes;
    }

    record EmoteList(List<EmoteView> emotes) {
    }

    record Uploaded(String id, String name, String url, String note) {
    }

    record Session(boolean guild, boolean admin) {
    }

    @GetMapping("/api/emotes")
    EmoteList approved() {
        return new EmoteList(emotes.approved().stream().map(EmoteView::of).toList());
    }

    @GetMapping("/api/check")
    DuplicateCheck check(@RequestParam(defaultValue = "") String name,
                         @RequestParam(defaultValue = "") String sha) {
        return emotes.check(name, sha);
    }

    @PostMapping(value = "/api/emotes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<Uploaded> upload(@RequestParam MultipartFile file,
                                    @RequestParam(defaultValue = "") String name,
                                    @RequestParam(defaultValue = "") String uploader,
                                    HttpServletRequest request) throws IOException {
        passcodes.requireGuild(request);

        String typedName = name.isBlank() ? stem(file.getOriginalFilename()) : name;
        Emote queued = emotes.upload(file.getBytes(), typedName, uploader, ClientIp.of(request));

        return ResponseEntity.status(201).body(new Uploaded(queued.id(), queued.name(),
                "/img/" + queued.fileName(), queued.note()));
    }

    /** Lets a page tell a wrong passcode from one that simply isn't the admin's. */
    @GetMapping("/api/session")
    Session session(HttpServletRequest request) {
        return new Session(passcodes.isGuild(request), passcodes.isAdmin(request));
    }

    private static String stem(String fileName) {
        return fileName == null ? "" : fileName.replaceFirst("\\.[^.]+$", "");
    }
}
