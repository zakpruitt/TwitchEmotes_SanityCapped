package com.zakpruitt.sanitycapped.web.api;

import com.zakpruitt.sanitycapped.emote.model.EmoteUpload;
import com.zakpruitt.sanitycapped.emote.service.EmoteService;
import com.zakpruitt.sanitycapped.web.dto.request.UploadRequest;
import com.zakpruitt.sanitycapped.web.dto.response.CheckResponse;
import com.zakpruitt.sanitycapped.web.dto.response.EmoteListResponse;
import com.zakpruitt.sanitycapped.web.dto.response.SessionResponse;
import com.zakpruitt.sanitycapped.web.dto.response.UploadResponse;
import com.zakpruitt.sanitycapped.web.security.Passcodes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
class EmoteApiController {

    private final EmoteService emotes;
    private final Passcodes passcodes;


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
