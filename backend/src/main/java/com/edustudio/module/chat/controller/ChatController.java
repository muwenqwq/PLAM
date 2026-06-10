package com.edustudio.module.chat.controller;

import com.edustudio.common.api.PageResult;
import com.edustudio.common.api.Result;
import com.edustudio.module.chat.dto.ChatMessageRequest;
import com.edustudio.module.chat.dto.ConversationCreateRequest;
import com.edustudio.module.chat.dto.ConversationQueryRequest;
import com.edustudio.module.chat.service.ChatService;
import com.edustudio.module.chat.vo.ChatResponseVO;
import com.edustudio.module.chat.vo.ConversationMessageVO;
import com.edustudio.module.chat.vo.ConversationVO;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "智能对话")
@Validated
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

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

    @Operation(summary = "发送对话消息")
    @PostMapping("/conversations/{id}/messages")
    public Result<ChatResponseVO> sendMessage(
            @PathVariable Long id,
            @Valid @RequestBody ChatMessageRequest request
    ) {
        return Result.success(chatService.sendMessage(id, request));
    }

    @Operation(summary = "识别聊天意图")
    @PostMapping("/intent")
    public Result<JsonNode> detectIntent(@Valid @RequestBody ChatMessageRequest request) {
        return Result.success(chatService.detectIntent(request));
    }
}
