package gg.sanitycapped.emotes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class EmotesApplication {

    public static void main(String[] args) throws IOException {
        // SQLite won't create the directory its file lives in, and the datasource
        // is built before any bean of ours could do it.
        Files.createDirectories(Path.of(System.getenv().getOrDefault("DATA_DIR", "./data")));
        SpringApplication.run(EmotesApplication.class, args);
    }
}
