package gg.sanitycapped.emotes;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

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
