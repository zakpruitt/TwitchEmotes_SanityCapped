package com.zakpruitt.sanitycapped.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/** Uploaded files in one directory on the data volume, named by id. */
public class DiskStore {

    private final Path directory;

    protected DiskStore(Path directory) {
        this.directory = directory;
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
            throw new IllegalArgumentException("Not a file in this store: " + fileName);
        }
        return file;
    }
}
