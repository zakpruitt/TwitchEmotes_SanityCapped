package gg.sanitycapped.emotes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import gg.sanitycapped.emotes.github.GitHubClient;

/**
 * A real application context on a throwaway data directory, with GitHub mocked
 * so no test can reach the network or commit anything.
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class WebTestBase {

    static final String PASSCODE_HEADER = "X-Passcode";
    static final String GUILD = "guild-test";
    static final String ADMIN = "admin-test";

    @MockitoBean
    GitHubClient github;

    @DynamicPropertySource
    static void testConfiguration(DynamicPropertyRegistry registry) throws IOException {
        Path dataDir = Files.createTempDirectory("emotes-test");
        registry.add("app.data-dir", dataDir::toString);
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + dataDir.resolve("test.db"));
        registry.add("app.guild-passcode", () -> GUILD);
        registry.add("app.admin-passcode", () -> ADMIN);
        registry.add("app.github-token", () -> "test-token");
    }
}
