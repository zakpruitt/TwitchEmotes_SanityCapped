package com.zakpruitt.sanitycapped.web.api;

import com.zakpruitt.sanitycapped.image.ImageStore;
import com.zakpruitt.sanitycapped.image.ImageType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
class ImageController {

    /** The filename carries a uuid, so a URL never changes content. */
    private static final CacheControl FOREVER =
            CacheControl.maxAge(Duration.ofDays(365)).immutable().cachePublic();

    private final ImageStore images;

    @GetMapping("/img/{fileName}")
    ResponseEntity<byte[]> image(@PathVariable String fileName) {
        return images.get(fileName)
                .map(data -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, ImageType.contentTypeOf(fileName))
                        .cacheControl(FOREVER)
                        .body(data))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
