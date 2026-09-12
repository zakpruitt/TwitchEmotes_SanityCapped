package gg.sanitycapped.emotes.emote;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.springframework.stereotype.Component;

import gg.sanitycapped.emotes.config.AppProperties;

/**
 * The uploaded originals, on this machine's disk. A cache, not the archive: the
 * approved ones also live in the repo, and a wiped volume refills from there.
 */
@Component
public class ImageStore {

    private final Path directory;

    ImageStore(AppProperties props) {
        this.directory = props.imageDir();
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create " + directory, e);
        }
    }

    public void put(String fileName, byte[] data) {
        try {
            Files.write(resolve(fileName), data);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write " + fileName, e);
        }
    }

    public Optional<byte[]> get(String fileName) {
        Path file = resolve(fileName);
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(file));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public void delete(String fileName) {
        try {
            Files.deleteIfExists(resolve(fileName));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot delete " + fileName, e);
        }
    }

    /** Names reach this from a URL, so refuse anything that climbs out of the directory. */
    private Path resolve(String fileName) {
        Path file = directory.resolve(fileName).normalize();
        if (!file.startsWith(directory)) {
            throw new IllegalArgumentException("Not a file in the image store: " + fileName);
        }
        return file;
    }
}
