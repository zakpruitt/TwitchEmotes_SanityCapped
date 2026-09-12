package gg.sanitycapped.emotes.web.api;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

import gg.sanitycapped.emotes.emote.EmoteService;
import gg.sanitycapped.emotes.image.ImageType;

/** Uploaded originals, served straight off this machine's disk. */
@RestController
class ImageController {

    private final EmoteService emotes;

    ImageController(EmoteService emotes) {
        this.emotes = emotes;
    }

    @GetMapping("/img/{fileName}")
    ResponseEntity<byte[]> image(@PathVariable String fileName) {
        return emotes.image(fileName)
                .map(data -> ResponseEntity.ok()
                        .header("Content-Type", ImageType.contentTypeOf(fileName))
                        // The filename carries a uuid, so a URL never changes content.
                        .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).immutable().cachePublic())
                        .body(data))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
