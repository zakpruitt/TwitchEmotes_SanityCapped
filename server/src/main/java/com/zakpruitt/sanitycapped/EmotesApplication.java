package com.zakpruitt.sanitycapped;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
@ConfigurationPropertiesScan
public class EmotesApplication {

    public static void main(String[] args) throws IOException {
        Files.createDirectories(Path.of(System.getenv().getOrDefault("DATA_DIR", "./data")));
        SpringApplication.run(EmotesApplication.class, args);
    }
}
