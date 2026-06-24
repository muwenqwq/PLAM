package com.edustudio.module.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edustudio.common.api.PageResult;
import com.edustudio.common.api.ResultCode;
import com.edustudio.common.exception.BusinessException;
import com.edustudio.common.security.LoginUserHolder;
import com.edustudio.common.utils.JsonUtils;
import com.edustudio.integration.ai.AiServiceClient;
import com.edustudio.integration.ai.dto.AiChatIntentRequest;
import com.edustudio.integration.ai.dto.AiChatRequest;
import com.edustudio.integration.ai.dto.AiChatResponse;
import com.edustudio.integration.ai.dto.AiModelConfigDTO;
import com.edustudio.module.chat.dto.ChatMessageRequest;
import com.edustudio.module.chat.dto.ConversationCreateRequest;
import com.edustudio.module.chat.dto.ConversationQueryRequest;
import com.edustudio.module.chat.entity.Conversation;
import com.edustudio.module.chat.entity.ConversationMessage;
import com.edustudio.module.chat.mapper.ConversationMapper;
import com.edustudio.module.chat.mapper.ConversationMessageMapper;
import com.edustudio.module.chat.service.ChatService;
import com.edustudio.module.chat.vo.ChatResponseVO;
import com.edustudio.module.chat.vo.ConversationMessageVO;
import com.edustudio.module.chat.vo.ConversationVO;
import com.edustudio.module.learningspace.service.LearningSpaceService;
import com.edustudio.module.modelprovider.service.ModelProviderService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final String ACTIVE_STATUS = "active";

    private final ConversationMapper conversationMapper;
    private final ConversationMessageMapper messageMapper;
    private final LearningSpaceService learningSpaceService;
    private final ModelProviderService modelProviderService;
    private final AiServiceClient aiServiceClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConversationVO createConversation(ConversationCreateRequest request) {
        Long userId = LoginUserHolder.requireCurrentUserId();
        if (request.getSpaceId() != null) {
            learningSpaceService.assertOwned(request.getSpaceId());
        }
        Conversation entity = new Conversation();
        entity.setUserId(userId);
        entity.setSpaceId(request.getSpaceId());
        entity.setTitle(StringUtils.hasText(request.getTitle()) ? request.getTitle() : "新的学习对话");
        entity.setIntentType(request.getIntentType());
        entity.setMessageCount(0);
        entity.setStatus(ACTIVE_STATUS);
        conversationMapper.insert(entity);
        return toConversationVO(entity);
    }

    @Override
    public PageResult<ConversationVO> page(ConversationQueryRequest request) {
        Long userId = LoginUserHolder.requireCurrentUserId();
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getUserId, userId)
                .eq(Conversation::getDeleted, 0)
                .eq(request.getSpaceId() != null, Conversation::getSpaceId, request.getSpaceId())
                .eq(StringUtils.hasText(request.getStatus()), Conversation::getStatus, request.getStatus())
                .like(StringUtils.hasText(request.getKeyword()), Conversation::getTitle, request.getKeyword())
                .orderByDesc(Conversation::getUpdatedAt);
        Page<Conversation> result = conversationMapper.selectPage(new Page<>(request.getPageNum(), request.getPageSize()), wrapper);
        List<ConversationVO> records = result.getRecords().stream().map(this::toConversationVO).toList();
        return PageResult.of(records, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public ConversationVO detail(Long id) {
        return toConversationVO(getOwnedConversation(id));
    }

    @Override
    public List<ConversationMessageVO> messages(Long conversationId) {
        getOwnedConversation(conversationId);
        return messageMapper.selectList(new LambdaQueryWrapper<ConversationMessage>()
                        .eq(ConversationMessage::getConversationId, conversationId)
                        .eq(ConversationMessage::getUserId, LoginUserHolder.requireCurrentUserId())
                        .eq(ConversationMessage::getDeleted, 0)
                        .orderByAsc(ConversationMessage::getCreatedAt))
                .stream()
                .map(this::toMessageVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatResponseVO sendMessage(Long conversationId, ChatMessageRequest request) {
        Conversation conversation = getOwnedConversation(conversationId);
        Long userId = LoginUserHolder.requireCurrentUserId();
        AiModelConfigDTO modelConfig = modelProviderService.resolveConfig(request.getModelProviderId());

        ConversationMessage userMessage = new ConversationMessage();
        userMessage.setConversationId(conversation.getId());
        userMessage.setUserId(userId);
        userMessage.setMessageRole("user");
        userMessage.setContentMd(request.getMessage());
        userMessage.setContentJson(JsonUtils.toJson(Map.of("subject", valueOrBlank(request.getSubject()))));
        userMessage.setTokenCount(Math.max(1, request.getMessage().length() / 2));
        userMessage.setStatus(ACTIVE_STATUS);
        messageMapper.insert(userMessage);

        AiChatResponse aiResponse = aiServiceClient.chat(AiChatRequest.builder()
                .modelConfig(modelConfig)
                .userId(userId)
                .conversationId(conversation.getId())
                .spaceId(conversation.getSpaceId())
                .subject(request.getSubject())
                .message(request.getMessage())
                .history(messages(conversationId).stream()
                        .map(message -> Map.of("role", message.getMessageRole(), "content", message.getContentMd()))
                        .toList())
                .profile(Map.of())
                .preference(Map.of())
                .build());

        ConversationMessage assistantMessage = new ConversationMessage();
        assistantMessage.setConversationId(conversation.getId());
        assistantMessage.setUserId(userId);
        assistantMessage.setMessageRole("assistant");
        assistantMessage.setContentMd(aiResponse.getReplyMarkdown());
        assistantMessage.setContentJson(aiResponse.getReplyJson() == null ? null : JsonUtils.toJson(aiResponse.getReplyJson()));
        assistantMessage.setTokenCount(aiResponse.getTokenCount() == null ? 0 : aiResponse.getTokenCount());
        assistantMessage.setStatus(ACTIVE_STATUS);
        messageMapper.insert(assistantMessage);

        conversation.setMessageCount((conversation.getMessageCount() == null ? 0 : conversation.getMessageCount()) + 2);
        if (conversation.getSummary() == null) {
            conversation.setSummary(shorten(aiResponse.getReplyMarkdown(), 300));
        }
        conversationMapper.updateById(conversation);

        return ChatResponseVO.builder()
                .conversation(toConversationVO(conversation))
                .userMessage(toMessageVO(userMessage))
                .assistantMessage(toMessageVO(assistantMessage))
                .build();
    }

    @Override
    public JsonNode detectIntent(ChatMessageRequest request) {
        AiModelConfigDTO modelConfig = modelProviderService.resolveConfig(request.getModelProviderId());
        return JsonUtils.fromJson(JsonUtils.toJson(aiServiceClient.detectIntent(AiChatIntentRequest.builder()
                .modelConfig(modelConfig)
                .message(request.getMessage())
                .subject(request.getSubject())
                .build())), JsonNode.class);
    }

    private Conversation getOwnedConversation(Long id) {
        Conversation entity = conversationMapper.selectOne(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getId, id)
                .eq(Conversation::getUserId, LoginUserHolder.requireCurrentUserId())
                .eq(Conversation::getDeleted, 0));
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "会话不存在或无权访问");
        }
        return entity;
    }

    private ConversationVO toConversationVO(Conversation entity) {
        return ConversationVO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .spaceId(entity.getSpaceId())
                .title(entity.getTitle())
                .intentType(entity.getIntentType())
                .summary(entity.getSummary())
                .messageCount(entity.getMessageCount())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private ConversationMessageVO toMessageVO(ConversationMessage entity) {
        return ConversationMessageVO.builder()
                .id(entity.getId())
                .conversationId(entity.getConversationId())
                .userId(entity.getUserId())
                .messageRole(entity.getMessageRole())
                .contentMd(entity.getContentMd())
                .contentJson(StringUtils.hasText(entity.getContentJson())
                        ? JsonUtils.fromJson(entity.getContentJson(), JsonNode.class)
                        : null)
                .tokenCount(entity.getTokenCount())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private String shorten(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String valueOrBlank(String value) {
        return value == null ? "" : value;
    }
}
