package com.edustudio.module.health.controller;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.sql.SQLException;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class HealthControllerTest {

    @Test
    void healthReturnsApplicationRuntimeInformation() throws Exception {
        HealthController controller = new HealthController("eduagent-studio-backend", new StandardEnvironment(), null);
        MockMvc mockMvc = standaloneSetup(controller).build();

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.applicationName").value("eduagent-studio-backend"))
                .andExpect(jsonPath("$.data.javaVersion").exists())
                .andExpect(jsonPath("$.data.activeProfiles").exists())
                .andExpect(jsonPath("$.data.currentTime").exists());
    }

    @Test
    void dbHealthReturnsConnectionFailureWithoutThrowing() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("database is down"));
        HealthController controller = new HealthController("eduagent-studio-backend", new StandardEnvironment(), dataSource);
        MockMvc mockMvc = standaloneSetup(controller).build();

        mockMvc.perform(get("/api/health/db"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.connected").value(false))
                .andExpect(jsonPath("$.data.message", containsString("database is down")));
    }
}
