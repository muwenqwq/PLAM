package com.edustudio.module.profile;

import com.edustudio.common.security.UserPrincipal;
import com.edustudio.module.profile.dto.UserProfileUpsertRequest;
import com.edustudio.module.profile.service.UserProfileService;
import com.edustudio.module.profile.vo.UserProfileVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserProfileService userProfileService;

    @Test
    void shouldRejectProfileEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/profiles/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401"));
    }

    @Test
    void shouldReturnCurrentUserProfileWhenLoggedIn() throws Exception {
        when(userProfileService.getMe()).thenReturn(UserProfileVO.empty(3L, null));

        mockMvc.perform(get("/api/profiles/me")
                        .with(authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.userId").value(3));
    }

    @Test
    void shouldUpdateCurrentUserProfileWhenLoggedIn() throws Exception {
        UserProfileVO response = UserProfileVO.builder()
                .id(11L)
                .userId(3L)
                .realName("张同学")
                .school("示例大学")
                .major("软件工程")
                .profileNarrative("我正在准备数据库期末考试，希望通过例题理解索引和事务。")
                .adaptiveSummary("当前目标是数据库期末复习，最近正在关注索引和事务。")
                .weeklyAvailableHours(new BigDecimal("8.00"))
                .status("active")
                .build();
        when(userProfileService.upsertMe(any(UserProfileUpsertRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/profiles/me")
                        .with(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "realName": "张同学",
                                  "school": "示例大学",
                                  "major": "软件工程",
                                  "profileNarrative": "我正在准备数据库期末考试，希望通过例题理解索引和事务。",
                                  "weeklyAvailableHours": 8
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.realName").value("张同学"))
                .andExpect(jsonPath("$.data.profileNarrative").value("我正在准备数据库期末考试，希望通过例题理解索引和事务。"));
    }

    @Test
    void shouldRejectInvalidProfileUpdateRequest() throws Exception {
        mockMvc.perform(put("/api/profiles/me")
                        .with(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "weeklyAvailableHours": -1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldReturnProfileBoundToOwnedLearningSpace() throws Exception {
        UserProfileVO response = UserProfileVO.empty(3L, 100L);
        when(userProfileService.getBySpace(100L)).thenReturn(response);

        mockMvc.perform(get("/api/profiles/space/100")
                        .with(authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.spaceId").value(100));
    }

    @Test
    void shouldReanalyzeProfileForOwnedLearningSpace() throws Exception {
        UserProfileVO response = UserProfileVO.builder()
                .userId(3L)
                .spaceId(100L)
                .adaptiveSummary("根据测验和学习对话，当前应优先巩固索引并采用分步骤讲解。")
                .profileSource("ai_adaptive")
                .build();
        when(userProfileService.reanalyzeBySpace(100L)).thenReturn(response);

        mockMvc.perform(post("/api/profiles/space/100/analyze")
                        .with(authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.profileSource").value("ai_adaptive"));
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
