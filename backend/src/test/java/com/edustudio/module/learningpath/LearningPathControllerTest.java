package com.edustudio.module.learningpath;

import com.edustudio.common.security.UserPrincipal;
import com.edustudio.module.learningpath.dto.LearningPathGenerateRequest;
import com.edustudio.module.learningpath.dto.LearningPathItemStatusRequest;
import com.edustudio.module.learningpath.dto.LearningPathUpdateRequest;
import com.edustudio.module.learningpath.service.LearningPathService;
import com.edustudio.module.learningpath.vo.LearningPathItemVO;
import com.edustudio.module.learningpath.vo.LearningPathVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LearningPathControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LearningPathService learningPathService;

    @Test
    void shouldRejectLearningPathWithoutToken() throws Exception {
        mockMvc.perform(get("/api/learning-paths"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldGenerateAndUpdateLearningPath() throws Exception {
        when(learningPathService.generate(any(LearningPathGenerateRequest.class))).thenReturn(LearningPathVO.builder().id(1L).title("Java 路径").build());
        when(learningPathService.updateItemStatus(eq(8L), any(LearningPathItemStatusRequest.class))).thenReturn(LearningPathItemVO.builder().id(8L).status("done").build());
        when(learningPathService.update(eq(1L), any(LearningPathUpdateRequest.class))).thenReturn(LearningPathVO.builder().id(1L).title("Java 路径（已编辑）").build());

        mockMvc.perform(post("/api/learning-paths/generate")
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"spaceId\":1,\"subject\":\"Java\",\"goal\":\"掌握后端\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Java 路径"));

        mockMvc.perform(put("/api/learning-path-items/8/status")
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"done\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("done"));

        mockMvc.perform(put("/api/learning-paths/1")
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Java 路径（已编辑）","subject":"Java","goal":"掌握后端",\
                                "items":[{"title":"Spring 入门","description":"完成一个接口",\
                                "knowledgePoints":["Spring"],"estimatedMinutes":45,"status":"todo"}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Java 路径（已编辑）"));
    }

    private static UsernamePasswordAuthenticationToken auth() {
        UserPrincipal principal = new UserPrincipal(3L, "demo_student", "", "演示学生", "active", List.of("STUDENT"));
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}
