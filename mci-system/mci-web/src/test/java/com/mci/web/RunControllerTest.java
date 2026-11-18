package com.mci.web;

import com.mci.core.Task;
import com.mci.core.TaskStatus;
import com.mci.persistence.RunDao;
import com.mci.persistence.SubTaskDao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class RunControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RunDao runDao;

    @Test
    void testGetSystemStats() throws Exception {
        mockMvc.perform(get("/api/system/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agentsActive").value(4));
    }

    @Test
    void testListRuns() throws Exception {
        mockMvc.perform(get("/api/runs"))
                .andExpect(status().isOk());
    }
}
