package com.zakpruitt.sanitycapped.image;

import com.zakpruitt.sanitycapped.config.AppProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/** Image files on the data volume, named by emote id. */
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

    public void put(String fileName, byte[] data) {
        try {
            Files.write(resolve(fileName), data);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write " + fileName, e);
        }
    }

    public void delete(String fileName) {
        try {
            Files.deleteIfExists(resolve(fileName));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot delete " + fileName, e);
        }
    }

    /** Names arrive from URLs, so refuse anything climbing out of the directory. */
    private Path resolve(String fileName) {
        Path file = directory.resolve(fileName).normalize();
        if (!file.startsWith(directory)) {
            throw new IllegalArgumentException("Not a file in the image store: " + fileName);
        }
        return file;
    }
}
