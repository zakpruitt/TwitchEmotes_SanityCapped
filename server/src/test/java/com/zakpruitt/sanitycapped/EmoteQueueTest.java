package com.zakpruitt.sanitycapped;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.awt.*;

import static com.zakpruitt.sanitycapped.TestImages.png;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


class EmoteQueueTest extends WebTestBase {

    @Autowired
    MockMvc mvc;

    private static String idOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
    }

    @Test
    void uploadingNeedsNoPasscode() throws Exception {
        mvc.perform(multipart("/api/emotes").file(png(Color.ORANGE))
                        .param("name", "openDoor").param("uploader", "zak"))
                .andExpect(status().isCreated());
    }

    @Test
    void theNameIsTheOneTheBuildWouldProduce() throws Exception {
        upload(png(Color.RED), "peepo-Hmm-128")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("scPeepoHmm"));
    }

    @Test
    void theSameImageCannotBeUploadedTwice() throws Exception {
        upload(png(Color.BLUE), "firstOne").andExpect(status().isCreated());

        upload(png(Color.BLUE), "secondOne")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("That exact image is already here as scFirstOne."));
    }

    @Test
    void twoEmotesCannotShareATriggerWord() throws Exception {
        upload(png(Color.GREEN), "sameName").andExpect(status().isCreated());

        upload(png(Color.YELLOW), "sameName")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("scSameName is already waiting for approval."));
    }

    @Test
    void somethingThatIsNotAnImageIsRefused() throws Exception {
        mvc.perform(multipart("/api/emotes")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "file", "evil.png", "image/png", "not an image".getBytes()))
                        .param("name", "evil").param("uploader", "zak"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("That isn't a GIF, PNG, WebP or JPEG."));
    }

    @Test
    void anUploadWithoutANameToThankIsRefused() throws Exception {
        mvc.perform(multipart("/api/emotes").file(png(Color.PINK))
                        .param("name", "anonymous").param("uploader", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Add your name so we know who to thank."));
    }

    @Test
    void theQueueNeedsTheAdminPasscode() throws Exception {
        mvc.perform(get("/api/admin/pending").header(PASSCODE_HEADER, "wrong"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void approvingCommitsTheImageAndPublishesTheEmote() throws Exception {
        given(github.putSource(anyString(), any(), anyString())).willReturn("blob-sha");

        String id = idOf(upload(png(Color.MAGENTA), "shipIt").andExpect(status().isCreated()));

        mvc.perform(get("/api/emotes"))
                .andExpect(jsonPath("$.emotes[?(@.name == 'scShipIt')]").isEmpty());

        mvc.perform(post("/api/admin/{id}/approve", id).header(PASSCODE_HEADER, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("approved"))
                .andExpect(jsonPath("$.name").value("scShipIt"));

        // Approving is the publish step: the commit is what builds the release.
        verify(github).putSource(eq("scShipIt.png"), any(), contains("uploaded by zak"));

        mvc.perform(get("/api/emotes"))
                .andExpect(jsonPath("$.emotes[?(@.name == 'scShipIt')]").isNotEmpty());
    }

    @Test
    void rejectingFreesTheNameForSomeoneElse() throws Exception {
        String id = idOf(upload(png(Color.CYAN), "tryAgain").andExpect(status().isCreated()));

        mvc.perform(post("/api/admin/{id}/reject", id).header(PASSCODE_HEADER, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "blurry"}
                                """))
                .andExpect(status().isOk());

        mvc.perform(get("/api/check").param("name", "tryAgain"))
                .andExpect(jsonPath("$.nameTaken").doesNotExist())
                .andExpect(jsonPath("$.name").value("scTryAgain"));
    }

    private ResultActions upload(org.springframework.mock.web.MockMultipartFile file, String name)
            throws Exception {
        return mvc.perform(multipart("/api/emotes").file(file)
                .param("name", name)
                .param("uploader", "zak"));
    }
}
