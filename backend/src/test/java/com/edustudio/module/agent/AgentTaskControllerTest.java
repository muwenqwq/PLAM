package com.edustudio.module.agent;

import com.edustudio.common.security.UserPrincipal;
import com.edustudio.module.agent.dto.AgentTaskCreateRequest;
import com.edustudio.module.agent.service.AgentTaskService;
import com.edustudio.module.agent.vo.AgentStepVO;
import com.edustudio.module.agent.vo.AgentTaskResultVO;
import com.edustudio.module.agent.vo.AgentTaskVO;
import com.edustudio.module.resource.vo.GeneratedResourceVO;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AgentTaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AgentTaskService agentTaskService;

    @Test
    void shouldRejectAgentTaskEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/agent-tasks"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401"));
    }

    @Test
    void shouldCreateAgentTaskWithStepsAndResource() throws Exception {
        when(agentTaskService.createAndRun(any(AgentTaskCreateRequest.class))).thenReturn(AgentTaskResultVO.builder()
                .task(AgentTaskVO.builder().id(1L).executionStatus("succeeded").title("生成复习计划").build())
                .steps(List.of(AgentStepVO.builder().agentName("PlannerAgent").stepOrder(1).build()))
                .resources(List.of(GeneratedResourceVO.builder().id(9L).title("数据库系统学习资源").build()))
                .build());

        mockMvc.perform(post("/api/agent-tasks")
                        .with(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "spaceId": 1,
                                  "taskType": "resource_generation",
                                  "title": "生成复习计划",
                                  "subject": "数据库系统",
                                  "resourceType": "plan"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.task.executionStatus").value("succeeded"))
                .andExpect(jsonPath("$.data.steps[0].agentName").value("PlannerAgent"))
                .andExpect(jsonPath("$.data.resources[0].title").value("数据库系统学习资源"));
    }

    private static RequestPostProcessor authenticatedUser() {
        UserPrincipal principal = new UserPrincipal(3L, "demo_student", "", "演示学生", "active", List.of("STUDENT"));
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
