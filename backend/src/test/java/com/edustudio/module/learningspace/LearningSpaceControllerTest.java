package com.edustudio.module.learningspace;

import com.edustudio.common.api.PageResult;
import com.edustudio.common.security.UserPrincipal;
import com.edustudio.module.learningspace.dto.LearningSpaceCreateRequest;
import com.edustudio.module.learningspace.dto.LearningSpaceQueryRequest;
import com.edustudio.module.learningspace.service.LearningSpaceService;
import com.edustudio.module.learningspace.vo.LearningSpaceVO;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LearningSpaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LearningSpaceService learningSpaceService;

    @Test
    void shouldRejectLearningSpaceEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/learning-spaces"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401"));
    }

    @Test
    void shouldAllowHealthWithoutToken() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }

    @Test
    void shouldCreateLearningSpaceWhenLoggedIn() throws Exception {
        LearningSpaceVO response = LearningSpaceVO.builder()
                .id(100L)
                .spaceName("数据库系统复习")
                .subject("数据库系统")
                .visibility("private")
                .defaultSpace(true)
                .status("active")
                .build();
        when(learningSpaceService.create(any(LearningSpaceCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/learning-spaces")
                        .with(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "spaceName": "数据库系统复习",
                                  "subject": "数据库系统",
                                  "visibility": "private"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.id").value(100))
                .andExpect(jsonPath("$.data.isDefault").value(true));
    }

    @Test
    void shouldQueryCurrentUserLearningSpacesWithPagination() throws Exception {
        LearningSpaceVO record = LearningSpaceVO.builder()
                .id(100L)
                .spaceName("数据库系统复习")
                .subject("数据库系统")
                .visibility("private")
                .status("active")
                .build();
        when(learningSpaceService.page(any(LearningSpaceQueryRequest.class)))
                .thenReturn(PageResult.of(List.of(record), 1, 1, 10));

        mockMvc.perform(get("/api/learning-spaces?pageNum=1&pageSize=10")
                        .with(authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(100));
        verify(learningSpaceService).page(any(LearningSpaceQueryRequest.class));
    }

    @Test
    void shouldRejectInvalidLearningSpaceCreateRequest() throws Exception {
        mockMvc.perform(post("/api/learning-spaces")
                        .with(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subject": "数据库系统"
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
