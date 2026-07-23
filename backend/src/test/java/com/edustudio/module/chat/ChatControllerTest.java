package com.edustudio.module.chat;

import com.edustudio.common.security.UserPrincipal;
import com.edustudio.module.chat.dto.ChatMessageRequest;
import com.edustudio.module.chat.service.ChatService;
import com.edustudio.module.chat.vo.ChatResponseVO;
import com.edustudio.module.chat.vo.ConversationMessageVO;
import com.edustudio.module.chat.vo.ConversationVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatService chatService;

    @Test
    void shouldRejectChatEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/chat/conversations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401"));
    }

    @Test
    void shouldStreamAssistantMessageAsServerSentEvents() throws Exception {
        ChatResponseVO response = ChatResponseVO.builder()
                .conversation(ConversationVO.builder().id(1L).title("database review").build())
                .userMessage(ConversationMessageVO.builder().id(11L).messageRole("user").contentMd("review index").build())
                .assistantMessage(ConversationMessageVO.builder().id(12L).messageRole("assistant").contentMd("Review B+Tree first, then practice query optimization.").build())
                .build();
        when(chatService.streamMessage(eq(1L), any(ChatMessageRequest.class), any())).thenAnswer(invocation -> {
            Consumer<String> onDelta = invocation.getArgument(2);
            onDelta.accept("Review B+Tree first, ");
            onDelta.accept("then practice query optimization.");
            return response;
        });

        MvcResult result = mockMvc.perform(post("/api/chat/conversations/1/messages/stream")
                        .with(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subject": "database review",
                                  "message": "review index"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"type\":\"delta\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Review B+Tree first")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"type\":\"done\"")));
    }

    @Test
    void shouldReturnUserAndAiMessagesWhenSendingMessage() throws Exception {
        when(chatService.sendMessage(eq(1L), any(ChatMessageRequest.class))).thenReturn(ChatResponseVO.builder()
                .conversation(ConversationVO.builder().id(1L).title("数据库复习").build())
                .userMessage(ConversationMessageVO.builder().id(11L).messageRole("user").contentMd("帮我复习索引").build())
                .assistantMessage(ConversationMessageVO.builder().id(12L).messageRole("assistant").contentMd("建议先理解 B+Tree。").build())
                .build());

        mockMvc.perform(post("/api/chat/conversations/1/messages")
                        .with(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subject": "数据库系统",
                                  "message": "帮我复习索引"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userMessage.messageRole").value("user"))
                .andExpect(jsonPath("$.data.assistantMessage.messageRole").value("assistant"));
    }

    private static RequestPostProcessor authenticatedUser() {
        UserPrincipal principal = new UserPrincipal(3L, "demo_student", "", "演示学生", "active", List.of("STUDENT"));
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
