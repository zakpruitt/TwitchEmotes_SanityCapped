package gg.sanitycapped.emotes.store;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.springframework.stereotype.Component;

import gg.sanitycapped.emotes.AppProperties;

/** The uploaded originals, on this machine's disk. */
@Component
public class ImageStore {

    private final Path dir;

    ImageStore(AppProperties props) {
        this.dir = props.imageDir();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create " + dir, e);
        }
    }

    public void put(String fileName, byte[] data) {
        try {
            Files.write(dir.resolve(fileName), data);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write " + fileName, e);
        }
    }

    public Optional<byte[]> get(String fileName) {
        Path file = dir.resolve(fileName).normalize();
        if (!file.startsWith(dir) || !Files.isRegularFile(file)) {
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
            Files.deleteIfExists(dir.resolve(fileName).normalize());
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot delete " + fileName, e);
        }
    }
}
