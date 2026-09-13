package gg.sanitycapped.emotes.web.dto;

import jakarta.validation.constraints.NotBlank;

import org.springframework.web.multipart.MultipartFile;

public record UploadRequest(MultipartFile file, String name, @NotBlank String uploader) {
}
