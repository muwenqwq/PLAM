package com.edustudio.module.chat.controller;

import com.edustudio.common.api.PageResult;
import com.edustudio.common.api.Result;
import com.edustudio.module.chat.dto.ChatMessageRequest;
import com.edustudio.module.chat.dto.ConversationCreateRequest;
import com.edustudio.module.chat.dto.ConversationQueryRequest;
import com.edustudio.module.chat.dto.ConversationRoleRequest;
import com.edustudio.module.chat.service.ChatService;
import com.edustudio.module.chat.vo.ChatResponseVO;
import com.edustudio.module.chat.vo.ConversationMessageVO;
import com.edustudio.module.chat.vo.ConversationVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "智能对话")
@Validated
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "创建会话")
    @PostMapping("/conversations")
    public Result<ConversationVO> createConversation(@Valid @RequestBody ConversationCreateRequest request) {
        return Result.success(chatService.createConversation(request));
    }

    @Operation(summary = "分页查询会话")
    @GetMapping("/conversations")
    public Result<PageResult<ConversationVO>> page(@Valid ConversationQueryRequest request) {
        return Result.success(chatService.page(request));
    }

    @Operation(summary = "查询会话详情")
    @GetMapping("/conversations/{id}")
    public Result<ConversationVO> detail(@PathVariable Long id) {
        return Result.success(chatService.detail(id));
    }

    @Operation(summary = "查询会话消息")
    @GetMapping("/conversations/{id}/messages")
    public Result<List<ConversationMessageVO>> messages(@PathVariable Long id) {
        return Result.success(chatService.messages(id));
    }

    @Operation(summary = "设置会话 AI 角色")
    @PostMapping("/conversations/{id}/role")
    public Result<ConversationVO> applyRole(
            @PathVariable Long id,
            @RequestBody ConversationRoleRequest request
    ) {
        return Result.success(chatService.applyRole(id, request));
    }

    @Operation(summary = "发送对话消息")
    @PostMapping("/conversations/{id}/messages")
    public Result<ChatResponseVO> sendMessage(
            @PathVariable Long id,
            @Valid @RequestBody ChatMessageRequest request
    ) {
        return Result.success(chatService.sendMessage(id, request));
    }


    @Operation(summary = "流式发送对话消息")
    @PostMapping(value = "/conversations/{id}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> streamMessage(
            @PathVariable Long id,
            @Valid @RequestBody ChatMessageRequest request
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        StreamingResponseBody body = outputStream -> {
            SecurityContext previousContext = SecurityContextHolder.getContext();
            SecurityContext streamContext = SecurityContextHolder.createEmptyContext();
            streamContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(streamContext);
            try {
                ChatResponseVO response = chatService.streamMessage(id, request, chunk -> {
                    try {
                        writeSse(outputStream, "delta", Map.of("content", chunk));
                    } catch (java.io.IOException exception) {
                        throw new UncheckedIOException(exception);
                    }
                });
                Map<String, Object> done = new LinkedHashMap<>();
                done.put("response", response);
                writeSse(outputStream, "done", done);
            } catch (UncheckedIOException exception) {
                throw exception.getCause();
            } catch (Exception exception) {
                String message = exception.getMessage() == null ? "流式对话失败，请稍后重试" : exception.getMessage();
                writeSse(outputStream, "error", Map.of("message", message));
            } finally {
                SecurityContextHolder.setContext(previousContext);
            }
        };
        return ResponseEntity.ok()
                .header("Cache-Control", "no-cache")
                .header("X-Accel-Buffering", "no")
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(body);
    }

    @Operation(summary = "识别聊天意图")
    @PostMapping("/intent")
    public Result<JsonNode> detectIntent(@Valid @RequestBody ChatMessageRequest request) {
        return Result.success(chatService.detectIntent(request));
    }
    private void writeSse(java.io.OutputStream outputStream, String type, Map<String, Object> payload) throws java.io.IOException {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", type);
        event.putAll(payload);
        outputStream.write(("data: " + objectMapper.writeValueAsString(event) + "\n\n").getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }
}
