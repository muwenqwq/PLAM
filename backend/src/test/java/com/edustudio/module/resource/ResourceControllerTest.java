package com.edustudio.module.resource;

import com.edustudio.common.security.UserPrincipal;
import com.edustudio.module.resource.dto.ResourceGenerateRequest;
import com.edustudio.module.resource.service.ResourceService;
import com.edustudio.module.resource.vo.GeneratedResourceVO;
import com.edustudio.module.resource.vo.ResourceGenerateResultVO;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ResourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResourceService resourceService;

    @Test
    void shouldRejectResourceWithoutToken() throws Exception {
        mockMvc.perform(get("/api/resources"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401"));
    }

    @Test
    void shouldGenerateKnowledgeGraphResource() throws Exception {
        when(resourceService.generate(any(ResourceGenerateRequest.class))).thenReturn(ResourceGenerateResultVO.builder()
                .resource(GeneratedResourceVO.builder().id(2L).resourceType("knowledge_graph").title("知识图谱").build())
                .message("ok")
                .build());

        mockMvc.perform(post("/api/resources/generate")
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"spaceId\":1,\"title\":\"知识图谱\",\"resourceType\":\"knowledge_graph\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resource.resourceType").value("knowledge_graph"));
    }

    private static UsernamePasswordAuthenticationToken auth() {
        UserPrincipal principal = new UserPrincipal(3L, "demo_student", "", "演示学生", "active", List.of("STUDENT"));
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}
