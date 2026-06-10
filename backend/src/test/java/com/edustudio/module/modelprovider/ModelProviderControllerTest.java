package com.edustudio.module.modelprovider;

import com.edustudio.common.api.PageResult;
import com.edustudio.common.security.UserPrincipal;
import com.edustudio.module.modelprovider.dto.ModelProviderCreateRequest;
import com.edustudio.module.modelprovider.dto.ModelProviderQueryRequest;
import com.edustudio.module.modelprovider.dto.ModelProviderTestRequest;
import com.edustudio.module.modelprovider.service.ModelProviderService;
import com.edustudio.module.modelprovider.vo.ModelProviderTestVO;
import com.edustudio.module.modelprovider.vo.ModelProviderVO;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ModelProviderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ModelProviderService modelProviderService;

    @Test
    void shouldRejectModelProviderEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/model-providers"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401"));
    }

    @Test
    void shouldCreateMockProviderWithoutReturningPlainApiKey() throws Exception {
        when(modelProviderService.create(any(ModelProviderCreateRequest.class))).thenReturn(ModelProviderVO.builder()
                .id(10L)
                .providerName("Mock 模型")
                .providerType("mock")
                .apiKeyMasked("MOCK-ONLY")
                .modelName("mock-chat-v1")
                .defaultProvider(true)
                .status("active")
                .build());

        mockMvc.perform(post("/api/model-providers")
                        .with(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "providerName": "Mock 模型",
                                  "providerType": "mock",
                                  "modelName": "mock-chat-v1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.apiKeyMasked").value("MOCK-ONLY"))
                .andExpect(jsonPath("$.data.apiKeyEncrypted").doesNotExist());
    }

    @Test
    void shouldTestMockProviderSuccessfully() throws Exception {
        when(modelProviderService.test(eq(10L), any(ModelProviderTestRequest.class))).thenReturn(ModelProviderTestVO.builder()
                .providerId(10L)
                .success(true)
                .providerType("mock")
                .modelName("mock-chat-v1")
                .message("Mock 模型连接成功")
                .build());

        mockMvc.perform(post("/api/model-providers/10/test")
                        .with(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true));
    }

    @Test
    void shouldPageCurrentUserProviders() throws Exception {
        when(modelProviderService.page(any(ModelProviderQueryRequest.class))).thenReturn(PageResult.of(List.of(
                ModelProviderVO.builder().id(10L).providerName("Mock 模型").providerType("mock").build()
        ), 1, 1, 10));

        mockMvc.perform(get("/api/model-providers").with(authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    private static RequestPostProcessor authenticatedUser() {
        UserPrincipal principal = new UserPrincipal(3L, "demo_student", "", "演示学生", "active", List.of("STUDENT"));
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
