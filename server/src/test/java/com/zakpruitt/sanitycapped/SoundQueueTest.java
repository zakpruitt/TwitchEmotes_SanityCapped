package com.zakpruitt.sanitycapped;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.concurrent.atomic.AtomicInteger;

import static com.zakpruitt.sanitycapped.TestSounds.mp3;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SoundQueueTest extends WebTestBase {

    /** Every test shares one context, so each upload comes from its own address to stay under the hourly limit. */
    private static final AtomicInteger UPLOADERS = new AtomicInteger();

    @Autowired
    MockMvc mvc;

    private static String freshIp() {
        return "10.1.0." + UPLOADERS.incrementAndGet();
    }

    private static String read(ResultActions result, String path) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
    }

    @Test
    void theNameIsTheOneTheBuildWouldProduce() throws Exception {
        upload(mp3(1, 1), "vine boom!")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("scVineboom"));
    }

    @Test
    void theUploadedSoundCanBePlayedBack() throws Exception {
        String url = read(upload(mp3(1, 2), "playMe").andExpect(status().isCreated()), "$.url");

        mvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "audio/mpeg"))
                .andExpect(content().bytes(TestSounds.bytes(1, 2)));
    }

    @Test
    void somethingThatIsNotAnMp3IsRefused() throws Exception {
        mvc.perform(multipart("/api/sounds")
                        .file(new MockMultipartFile("file", "evil.mp3", "audio/mpeg", "not audio".getBytes()))
                        .header("X-Forwarded-For", freshIp())
                        .param("name", "evil").param("uploader", "zak"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("That isn't an MP3."));
    }

    @Test
    void aSoundThatRunsTooLongIsRefused() throws Exception {
        upload(mp3(25, 3), "wholeSong")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("That sound is 25 seconds long. Keep it under 20."));
    }

    @Test
    void theSameSoundCannotBeUploadedTwice() throws Exception {
        upload(mp3(1, 4), "firstSound").andExpect(status().isCreated());

        upload(mp3(1, 4), "secondSound")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("That exact sound is already here as scFirstSound."));
    }

    @Test
    void twoSoundsCannotShareACommandWhateverTheirCapitals() throws Exception {
        upload(mp3(1, 5), "sameCommand").andExpect(status().isCreated());

        // Slash commands ignore case, so these would both be /scsamecommand.
        upload(mp3(1, 6), "SAMEcommand")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("scSameCommand is already waiting for approval."));

        mvc.perform(get("/api/sounds/check").param("name", "samecommand"))
                .andExpect(jsonPath("$.nameTaken").value("pending"));
    }

    @Test
    void aSoundAndAnEmoteCanShareAName() throws Exception {
        mvc.perform(multipart("/api/emotes").file(TestImages.png(java.awt.Color.DARK_GRAY))
                        .header("X-Forwarded-For", freshIp())
                        .param("name", "sharedName").param("uploader", "zak"))
                .andExpect(status().isCreated());

        upload(mp3(1, 7), "sharedName").andExpect(status().isCreated());
    }

    @Test
    void theSoundQueueNeedsTheAdminPasscode() throws Exception {
        mvc.perform(get("/api/admin/sounds/pending").header(PASSCODE_HEADER, "wrong"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void approvingCommitsTheMp3AndPublishesTheSound() throws Exception {
        given(github.putSound(anyString(), any(), anyString())).willReturn("blob-sha");

        String id = read(upload(mp3(2, 8), "shipTheSound").andExpect(status().isCreated()), "$.id");

        mvc.perform(get("/api/admin/sounds/pending").header(PASSCODE_HEADER, ADMIN))
                .andExpect(jsonPath("$.pending[?(@.name == 'scShipTheSound')].command")
                        .value("/scshipthesound"));
        mvc.perform(get("/api/sounds"))
                .andExpect(jsonPath("$.sounds[?(@.name == 'scShipTheSound')]").isEmpty());

        mvc.perform(post("/api/admin/sounds/{id}/approve", id).header(PASSCODE_HEADER, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("approved"))
                .andExpect(jsonPath("$.name").value("scShipTheSound"));

        // It goes into the sound pack's folder, never the emote source folder.
        verify(github).putSound(eq("scShipTheSound.mp3"), any(), contains("uploaded by zak"));
        verify(github, never()).putSource(eq("scShipTheSound.mp3"), any(), anyString());

        mvc.perform(get("/api/sounds"))
                .andExpect(jsonPath("$.sounds[?(@.name == 'scShipTheSound')]").isNotEmpty());
    }

    @Test
    void removingAnApprovedSoundDeletesItFromTheRepo() throws Exception {
        given(github.putSound(anyString(), any(), anyString())).willReturn("blob-sha");
        String id = read(upload(mp3(1, 9), "shortLived").andExpect(status().isCreated()), "$.id");
        mvc.perform(post("/api/admin/sounds/{id}/approve", id).header(PASSCODE_HEADER, ADMIN))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/admin/sounds/{id}", id).header(PASSCODE_HEADER, ADMIN))
                .andExpect(status().isOk());

        verify(github).deleteSound(eq("scShortLived.mp3"), eq("blob-sha"), contains("scShortLived"));
        mvc.perform(get("/api/sounds"))
                .andExpect(jsonPath("$.sounds[?(@.name == 'scShortLived')]").isEmpty());
    }

    @Test
    void rejectingFreesTheNameForSomeoneElse() throws Exception {
        String id = read(upload(mp3(1, 10), "tryThisAgain").andExpect(status().isCreated()), "$.id");

        mvc.perform(post("/api/admin/sounds/{id}/reject", id).header(PASSCODE_HEADER, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "too loud"}
                                """))
                .andExpect(status().isOk());

        mvc.perform(get("/api/sounds/check").param("name", "tryThisAgain"))
                .andExpect(jsonPath("$.nameTaken").doesNotExist())
                .andExpect(jsonPath("$.name").value("scTryThisAgain"));
    }

    private ResultActions upload(MockMultipartFile file, String name) throws Exception {
        return mvc.perform(multipart("/api/sounds").file(file)
                .header("X-Forwarded-For", freshIp())
                .param("name", name)
                .param("uploader", "zak"));
    }
}
