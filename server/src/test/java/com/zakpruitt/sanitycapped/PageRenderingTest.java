package com.zakpruitt.sanitycapped;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PageRenderingTest extends WebTestBase {

    @Autowired
    MockMvc mvc;

    @Test
    void browseRendersTheLayoutAndTheInstallUrl() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("browse"))
                .andExpect(model().attributeExists("emotes"))
                // From the layout fragment, so the base template really is applied.
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sanity Capped")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "https://github.com/zakpruitt/TwitchEmotes_SanityCapped")));
    }

    @Test
    void soundsPageRendersWithALinkInTheNav() throws Exception {
        mvc.perform(get("/sounds"))
                .andExpect(status().isOk())
                .andExpect(view().name("sounds"))
                .andExpect(model().attributeExists("sounds"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("sound pack")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/sounds\"")));
    }

    @Test
    void uploadPageStatesTheSizeAndLengthLimits() throws Exception {
        mvc.perform(get("/upload"))
                .andExpect(status().isOk())
                .andExpect(view().name("upload"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("2</span> MB")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("20</span> seconds")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-max-sound-seconds=\"20\"")));
    }

    @Test
    void theLogoIsServedAndNotTakenForAnEmote() throws Exception {
        // /img/** belongs to uploaded emotes, so brand assets must live elsewhere.
        mvc.perform(get("/brand/crown.png"))
                .andExpect(status().isOk());
    }

    @Test
    void adminPageRenders() throws Exception {
        mvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Approval queue")));
    }
}
