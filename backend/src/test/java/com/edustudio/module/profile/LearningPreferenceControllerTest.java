package com.edustudio.module.profile;

import com.edustudio.common.security.UserPrincipal;
import com.edustudio.module.profile.dto.LearningPreferenceUpsertRequest;
import com.edustudio.module.profile.service.LearningPreferenceService;
import com.edustudio.module.profile.vo.LearningPreferenceVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LearningPreferenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LearningPreferenceService learningPreferenceService;

    @Test
    void shouldRejectPreferenceEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/preferences/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401"));
    }

    @Test
    void shouldReturnDefaultPreferenceWhenMissing() throws Exception {
        when(learningPreferenceService.getMe()).thenReturn(LearningPreferenceVO.defaults(3L));

        mockMvc.perform(get("/api/preferences/me")
                        .with(authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.outputStyle").value("markdown"))
                .andExpect(jsonPath("$.data.difficultyPreference").value("medium"))
                .andExpect(jsonPath("$.data.languagePreference").value("zh-CN"))
                .andExpect(jsonPath("$.data.knowledgeGraphEnabled").value(true))
                .andExpect(jsonPath("$.data.quizEnabled").value(true));
    }

    @Test
    void shouldUpdateCurrentUserPreferenceWhenLoggedIn() throws Exception {
        LearningPreferenceVO response = LearningPreferenceVO.defaults(3L).toBuilder()
                .contentLengthPreference("long")
                .reviewPlanEnabled(false)
                .build();
        when(learningPreferenceService.upsertMe(any(LearningPreferenceUpsertRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/preferences/me")
                        .with(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "outputStyle": "markdown",
                                  "difficultyPreference": "medium",
                                  "languagePreference": "zh-CN",
                                  "contentLengthPreference": "long",
                                  "knowledgeGraphEnabled": true,
                                  "quizEnabled": true,
                                  "reviewPlanEnabled": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.contentLengthPreference").value("long"))
                .andExpect(jsonPath("$.data.reviewPlanEnabled").value(false));
    }

    @Test
    void shouldRejectInvalidPreferenceUpdateRequest() throws Exception {
        mockMvc.perform(put("/api/preferences/me")
                        .with(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "difficultyPreference": "impossible"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.success").value(false));
    }

    private static RequestPostProcessor authenticatedUser() {
        UserPrincipal principal = new UserPrincipal(
                3L,
                "demo_student",
                "",
                "演示学生",
                "active",
                List.of("STUDENT")
        );
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        return authentication(token);
    }
}
