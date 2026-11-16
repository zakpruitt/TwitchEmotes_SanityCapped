package com.zakpruitt.sanitycapped.web.api;

import com.zakpruitt.sanitycapped.audio.SoundStore;
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
class AudioController {

    /** The filename carries a uuid, so a URL never changes content. */
    private static final CacheControl FOREVER =
            CacheControl.maxAge(Duration.ofDays(365)).immutable().cachePublic();

    private final SoundStore sounds;

    @GetMapping("/snd/{fileName}")
    ResponseEntity<byte[]> sound(@PathVariable String fileName) {
        return sounds.get(fileName)
                .map(data -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, "audio/mpeg")
                        .cacheControl(FOREVER)
                        .body(data))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
