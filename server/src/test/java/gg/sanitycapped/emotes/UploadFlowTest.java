package gg.sanitycapped.emotes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import gg.sanitycapped.emotes.github.GitHubClient;

/** The upload queue end to end, with GitHub stubbed out. */
@SpringBootTest
class UploadFlowTest {

    private static final String GUILD = "guild-test";
    private static final String ADMIN = "admin-test";

    @DynamicPropertySource
    static void config(DynamicPropertyRegistry registry) throws IOException {
        Path dir = java.nio.file.Files.createTempDirectory("emotes-test");
        registry.add("app.data-dir", dir::toString);
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + dir.resolve("test.db"));
        registry.add("app.guild-passcode", () -> GUILD);
        registry.add("app.admin-passcode", () -> ADMIN);
        registry.add("app.github-token", () -> "test-token");
    }

    @MockitoBean
    GitHubClient github;

    @Autowired
    WebApplicationContext context;

    MockMvc mvc() {
        return MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void uploadsAreQueuedAndDuplicatesAreRefused() throws Exception {
        MockMvc mvc = mvc();

        mvc.perform(multipart("/api/emotes").file(png(Color.RED, "file"))
                        .param("name", "peepo-Hmm-128").param("uploader", "zak"))
                .andExpect(status().isUnauthorized());

        String id = com.jayway.jsonpath.JsonPath.read(
                mvc.perform(multipart("/api/emotes").file(png(Color.RED, "file"))
                                .header("X-Passcode", GUILD)
                                .param("name", "peepo-Hmm-128").param("uploader", "zak"))
                        .andExpect(status().isCreated())
                        // The size suffix and the dash are stripped, exactly as the build would.
                        .andExpect(jsonPath("$.name").value("scPeepoHmm"))
                        .andReturn().getResponse().getContentAsString(),
                "$.id");

        mvc.perform(multipart("/api/emotes").file(png(Color.RED, "file"))
                        .header("X-Passcode", GUILD)
                        .param("name", "somethingElse").param("uploader", "zak"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("That exact image is already here as scPeepoHmm."));

        mvc.perform(multipart("/api/emotes").file(png(Color.BLUE, "file"))
                        .header("X-Passcode", GUILD)
                        .param("name", "peepoHmm").param("uploader", "zak"))
                .andExpect(status().isConflict());

        mvc.perform(multipart("/api/emotes").file(new MockMultipartFile("file", "evil.png",
                                "image/png", "not an image".getBytes()))
                        .header("X-Passcode", GUILD)
                        .param("name", "evil").param("uploader", "zak"))
                .andExpect(status().isBadRequest());

        // The guild passcode must not reach the queue.
        mvc.perform(get("/api/admin/pending").header("X-Passcode", GUILD))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/admin/pending").header("X-Passcode", ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pending.length()").value(1));

        // Nothing is public until it is approved.
        mvc.perform(get("/api/emotes")).andExpect(jsonPath("$.emotes.length()").value(0));

        given(github.putSource(anyString(), any(), anyString())).willReturn("blob-sha");

        mvc.perform(post("/api/admin/" + id).header("X-Passcode", ADMIN)
                        .contentType("application/json")
                        .content("{\"action\":\"approve\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("scPeepoHmm"));

        // Approval is the publish step: the image lands in tools/source/ named
        // after its trigger word, and that push is what builds the release.
        verify(github).putSource(org.mockito.ArgumentMatchers.eq("scPeepoHmm.png"), any(),
                org.mockito.ArgumentMatchers.contains("uploaded by zak"));

        mvc.perform(get("/api/emotes"))
                .andExpect(jsonPath("$.emotes.length()").value(1))
                .andExpect(jsonPath("$.emotes[0].name").value("scPeepoHmm"));
    }

    @Test
    void rejectingFreesTheNameAgain() throws Exception {
        MockMvc mvc = mvc();

        String id = com.jayway.jsonpath.JsonPath.read(
                mvc.perform(multipart("/api/emotes").file(png(Color.GREEN, "file"))
                                .header("X-Passcode", GUILD)
                                .param("name", "tryAgain").param("uploader", "zak"))
                        .andExpect(status().isCreated())
                        .andReturn().getResponse().getContentAsString(),
                "$.id");

        mvc.perform(post("/api/admin/" + id).header("X-Passcode", ADMIN)
                        .contentType("application/json")
                        .content("{\"action\":\"reject\",\"reason\":\"blurry\"}"))
                .andExpect(status().isOk());

        String check = mvc.perform(get("/api/check").param("name", "tryAgain"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(check).contains("\"nameTaken\":null");
    }

    /** A distinct little PNG per colour, so hashes differ between cases. */
    private static MockMultipartFile png(Color color, String field) throws IOException {
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, 32, 32);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 16, 8);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return new MockMultipartFile(field, "emote.png", "image/png", out.toByteArray());
    }
}
