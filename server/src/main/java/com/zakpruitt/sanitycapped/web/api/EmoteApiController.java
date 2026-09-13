package com.zakpruitt.sanitycapped.web.api;

import com.zakpruitt.sanitycapped.emote.dto.EmoteUpload;
import com.zakpruitt.sanitycapped.emote.service.DuplicateDetector;
import com.zakpruitt.sanitycapped.emote.service.EmoteQueryService;
import com.zakpruitt.sanitycapped.emote.service.EmoteUploadService;
import com.zakpruitt.sanitycapped.web.dto.request.UploadRequest;
import com.zakpruitt.sanitycapped.web.dto.response.CheckResponse;
import com.zakpruitt.sanitycapped.web.dto.response.EmoteListResponse;
import com.zakpruitt.sanitycapped.web.dto.response.SessionResponse;
import com.zakpruitt.sanitycapped.web.dto.response.UploadResponse;
import com.zakpruitt.sanitycapped.web.security.Passcodes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
class EmoteApiController {

    private final EmoteQueryService queries;
    private final EmoteUploadService uploads;
    private final DuplicateDetector duplicates;
    private final Passcodes passcodes;

    @GetMapping("/emotes")
    EmoteListResponse approved() {
        return EmoteListResponse.from(queries.approved());
    }

    @GetMapping("/check")
    CheckResponse check(@RequestParam(defaultValue = "") String name,
                        @RequestParam(defaultValue = "") String sha) {
        return CheckResponse.from(duplicates.check(name, sha));
    }

    @PostMapping(value = "/emotes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    UploadResponse upload(@Valid @ModelAttribute UploadRequest request, HttpServletRequest http)
            throws IOException {
        return UploadResponse.from(uploads.upload(toUpload(request, http)));
    }

    @GetMapping("/session")
    SessionResponse session(HttpServletRequest http) {
        return new SessionResponse(passcodes.isAdmin(http));
    }

    private static EmoteUpload toUpload(UploadRequest request, HttpServletRequest http) throws IOException {
        return new EmoteUpload(request.file().getBytes(), request.name(),
                request.file().getOriginalFilename(), request.uploader(), ClientIp.of(http));
    }
}
