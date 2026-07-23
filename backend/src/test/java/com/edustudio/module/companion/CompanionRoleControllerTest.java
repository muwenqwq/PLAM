package com.edustudio.module.companion;

import com.edustudio.common.api.PageResult;
import com.edustudio.common.security.UserPrincipal;
import com.edustudio.module.chat.dto.ConversationRoleRequest;
import com.edustudio.module.chat.service.ChatService;
import com.edustudio.module.chat.vo.ConversationVO;
import com.edustudio.module.companion.dto.CompanionRoleCreateRequest;
import com.edustudio.module.companion.dto.CompanionRoleQueryRequest;
import com.edustudio.module.companion.service.CompanionRoleService;
import com.edustudio.module.companion.vo.CompanionRoleVO;
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
class CompanionRoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompanionRoleService companionRoleService;

    @MockBean
    private ChatService chatService;

    @Test
    void shouldCreateCompanionRoleWithRichCustomization() throws Exception {
        when(companionRoleService.create(any(CompanionRoleCreateRequest.class))).thenReturn(CompanionRoleVO.builder()
                .id(21L)
                .roleName("温柔学姐小知")
                .roleIdentity("高年级学姐")
                .personality("温柔、耐心、积极反馈")
                .speakingStyle("鼓励式、伙伴式、循循善诱")
                .scenario("晚自习、考前冲刺")
                .companionGoal("缓解焦虑并督促打卡")
                .defaultRole(true)
                .status("active")
                .build());

        mockMvc.perform(post("/api/companion-roles")
                        .with(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roleName": "温柔学姐小知",
                                  "roleIdentity": "高年级学姐",
                                  "background": "正在陪你准备英语六级",
                                  "personality": "温柔、耐心、积极反馈",
                                  "expertise": "英语六级、学习规划",
                                  "hobbies": "做笔记、分享方法、轻音乐",
                                  "speakingStyle": "鼓励式、伙伴式、循循善诱",
                                  "scenario": "晚自习、考前冲刺",
                                  "companionGoal": "缓解焦虑并督促打卡",
                                  "boundaries": "不直接给作业答案，先引导思考",
                                  "customPrompt": "回答前先把任务拆成两步",
                                  "tags": "陪伴型,鼓励式,英语六级",
                                  "defaultRole": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roleName").value("温柔学姐小知"))
                .andExpect(jsonPath("$.data.speakingStyle").value("鼓励式、伙伴式、循循善诱"))
                .andExpect(jsonPath("$.data.defaultRole").value(true));
    }

    @Test
    void shouldListAndApplyCompanionRoleToConversation() throws Exception {
        when(companionRoleService.page(any(CompanionRoleQueryRequest.class))).thenReturn(PageResult.of(List.of(
                CompanionRoleVO.builder().id(21L).roleName("温柔学姐小知").defaultRole(true).status("active").build()
        ), 1, 1, 10));
        when(chatService.applyRole(eq(7L), any(ConversationRoleRequest.class))).thenReturn(ConversationVO.builder()
                .id(7L)
                .title("英语六级晚自习")
                .roleId(21L)
                .rolePlayEnabled(true)
                .roleName("温柔学姐小知")
                .build());

        mockMvc.perform(get("/api/companion-roles").with(authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].roleName").value("温柔学姐小知"));

        mockMvc.perform(post("/api/chat/conversations/7/role")
                        .with(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roleId": 21,
                                  "rolePlayEnabled": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roleId").value(21))
                .andExpect(jsonPath("$.data.rolePlayEnabled").value(true))
                .andExpect(jsonPath("$.data.roleName").value("温柔学姐小知"));
    }

    private static RequestPostProcessor authenticatedUser() {
        UserPrincipal principal = new UserPrincipal(3L, "demo_student", "", "演示学生", "active", List.of("STUDENT"));
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
