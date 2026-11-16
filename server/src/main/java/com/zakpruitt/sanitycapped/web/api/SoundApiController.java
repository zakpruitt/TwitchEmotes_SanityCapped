package com.zakpruitt.sanitycapped.web.api;

import com.zakpruitt.sanitycapped.sound.dto.SoundUpload;
import com.zakpruitt.sanitycapped.sound.service.SoundDuplicateDetector;
import com.zakpruitt.sanitycapped.sound.service.SoundQueryService;
import com.zakpruitt.sanitycapped.sound.service.SoundUploadService;
import com.zakpruitt.sanitycapped.web.dto.request.UploadRequest;
import com.zakpruitt.sanitycapped.web.dto.response.CheckResponse;
import com.zakpruitt.sanitycapped.web.dto.response.SoundListResponse;
import com.zakpruitt.sanitycapped.web.dto.response.UploadResponse;
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
@RequestMapping("/api/sounds")
@RequiredArgsConstructor
class SoundApiController {

    private final SoundQueryService queries;
    private final SoundUploadService uploads;
    private final SoundDuplicateDetector duplicates;

    @GetMapping
    SoundListResponse approved() {
        return SoundListResponse.from(queries.approved());
    }

    @GetMapping("/check")
    CheckResponse check(@RequestParam(defaultValue = "") String name,
                        @RequestParam(defaultValue = "") String sha) {
        return CheckResponse.from(duplicates.check(name, sha));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    UploadResponse upload(@Valid @ModelAttribute UploadRequest request, HttpServletRequest http)
            throws IOException {
        return UploadResponse.from(uploads.upload(toUpload(request, http)));
    }

    private static SoundUpload toUpload(UploadRequest request, HttpServletRequest http) throws IOException {
        return new SoundUpload(request.file().getBytes(), request.name(),
                request.file().getOriginalFilename(), request.uploader(), ClientIp.of(http));
    }
}
