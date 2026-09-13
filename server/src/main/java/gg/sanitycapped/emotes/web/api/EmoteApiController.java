package gg.sanitycapped.emotes.web.api;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gg.sanitycapped.emotes.emote.EmoteService;
import gg.sanitycapped.emotes.emote.EmoteUpload;
import gg.sanitycapped.emotes.web.Passcodes;
import gg.sanitycapped.emotes.web.dto.CheckResponse;
import gg.sanitycapped.emotes.web.dto.EmoteListResponse;
import gg.sanitycapped.emotes.web.dto.SessionResponse;
import gg.sanitycapped.emotes.web.dto.UploadRequest;
import gg.sanitycapped.emotes.web.dto.UploadResponse;

@RestController
class EmoteApiController {

    private final EmoteService emotes;
    private final Passcodes passcodes;

    EmoteApiController(EmoteService emotes, Passcodes passcodes) {
        this.emotes = emotes;
        this.passcodes = passcodes;
    }

    @GetMapping("/api/emotes")
    EmoteListResponse approved() {
        return EmoteListResponse.from(emotes.approved());
    }

    @GetMapping("/api/check")
    CheckResponse check(@RequestParam(defaultValue = "") String name,
                        @RequestParam(defaultValue = "") String sha) {
        return CheckResponse.from(emotes.check(name, sha));
    }

    @PostMapping(value = "/api/emotes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<UploadResponse> upload(@Valid @ModelAttribute UploadRequest request,
                                          HttpServletRequest http) throws IOException {
        passcodes.requireGuild(http);
        EmoteUpload upload = new EmoteUpload(request.file().getBytes(), request.name(),
                request.file().getOriginalFilename(), request.uploader(), ClientIp.of(http));
        return ResponseEntity.status(201).body(UploadResponse.from(emotes.upload(upload)));
    }

    @GetMapping("/api/session")
    SessionResponse session(HttpServletRequest http) {
        return new SessionResponse(passcodes.isGuild(http), passcodes.isAdmin(http));
    }
}
