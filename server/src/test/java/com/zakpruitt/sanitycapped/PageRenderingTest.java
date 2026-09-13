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
    void uploadPageStatesTheSizeLimit() throws Exception {
        mvc.perform(get("/upload"))
                .andExpect(status().isOk())
                .andExpect(view().name("upload"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("2</span> MB")));
    }

    @Test
    void adminPageRenders() throws Exception {
        mvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Approval queue")));
    }
}
