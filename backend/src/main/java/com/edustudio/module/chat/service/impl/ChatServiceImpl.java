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
import com.edustudio.module.chat.dto.ConversationRoleRequest;
import com.edustudio.module.chat.entity.Conversation;
import com.edustudio.module.chat.entity.ConversationMessage;
import com.edustudio.module.chat.mapper.ConversationMapper;
import com.edustudio.module.chat.mapper.ConversationMessageMapper;
import com.edustudio.module.chat.service.ChatService;
import com.edustudio.module.chat.vo.ChatResponseVO;
import com.edustudio.module.chat.vo.ConversationMessageVO;
import com.edustudio.module.chat.vo.ConversationVO;
import com.edustudio.module.companion.entity.CompanionRole;
import com.edustudio.module.companion.service.CompanionRoleService;
import com.edustudio.module.learningspace.service.LearningSpaceService;
import com.edustudio.module.modelprovider.service.ModelProviderService;
import com.edustudio.module.profile.service.UserProfileService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final String ACTIVE_STATUS = "active";

    private final ConversationMapper conversationMapper;
    private final ConversationMessageMapper messageMapper;
    private final LearningSpaceService learningSpaceService;
    private final ModelProviderService modelProviderService;
    private final CompanionRoleService companionRoleService;
    private final UserProfileService userProfileService;
    private final AiServiceClient aiServiceClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConversationVO createConversation(ConversationCreateRequest request) {
        Long userId = LoginUserHolder.requireCurrentUserId();
        if (request.getSpaceId() != null) {
            learningSpaceService.assertOwned(request.getSpaceId());
        }
        CompanionRole role = request.getRoleId() == null ? null : companionRoleService.getOwnedActiveRole(request.getRoleId());

        Conversation entity = new Conversation();
        entity.setUserId(userId);
        entity.setSpaceId(request.getSpaceId());
        entity.setTitle(StringUtils.hasText(request.getTitle()) ? request.getTitle() : "新的学习对话");
        entity.setIntentType(request.getIntentType());
        entity.setRoleId(role == null ? null : role.getId());
        entity.setRolePlayEnabled(role != null && Boolean.TRUE.equals(request.getRolePlayEnabled()));
        entity.setMessageCount(0);
        entity.setStatus(ACTIVE_STATUS);
        conversationMapper.insert(entity);
        return toConversationVO(entity, role);
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
        List<ConversationVO> records = result.getRecords().stream().map(entity -> toConversationVO(entity, null)).toList();
        return PageResult.of(records, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public ConversationVO detail(Long id) {
        Conversation conversation = getOwnedConversation(id);
        return toConversationVO(conversation, resolveActiveRole(conversation));
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
        return sendMessageInternal(conversationId, request, aiServiceClient::chat);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatResponseVO streamMessage(
            Long conversationId,
            ChatMessageRequest request,
            Consumer<String> onDelta
    ) {
        return sendMessageInternal(
                conversationId,
                request,
                aiRequest -> aiServiceClient.streamChat(aiRequest, onDelta)
        );
    }

    private ChatResponseVO sendMessageInternal(
            Long conversationId,
            ChatMessageRequest request,
            Function<AiChatRequest, AiChatResponse> aiCall
    ) {
        Conversation conversation = getOwnedConversation(conversationId);
        CompanionRole role = resolveActiveRole(conversation);
        Long userId = LoginUserHolder.requireCurrentUserId();
        AiModelConfigDTO modelConfig = modelProviderService.resolveConfig(request.getModelProviderId());
        List<Map<String, String>> history = messages(conversationId).stream()
                .map(message -> Map.of("role", message.getMessageRole(), "content", message.getContentMd()))
                .toList();

        ConversationMessage userMessage = new ConversationMessage();
        userMessage.setConversationId(conversation.getId());
        userMessage.setUserId(userId);
        userMessage.setMessageRole("user");
        userMessage.setContentMd(request.getMessage());
        userMessage.setContentJson(JsonUtils.toJson(Map.of(
                "subject", valueOrBlank(request.getSubject()),
                "role_play_enabled", role != null
        )));
        userMessage.setTokenCount(Math.max(1, request.getMessage().length() / 2));
        userMessage.setStatus(ACTIVE_STATUS);
        messageMapper.insert(userMessage);

        Map<String, Object> roleMap = roleToMap(role);
        Map<String, Object> learnerProfile = userProfileService.getAiProfile(conversation.getSpaceId());
        AiChatResponse aiResponse = aiCall.apply(AiChatRequest.builder()
                .modelConfig(modelConfig)
                .userId(userId)
                .conversationId(conversation.getId())
                .spaceId(conversation.getSpaceId())
                .subject(request.getSubject())
                .message(request.getMessage())
                .history(history)
                .profile(learnerProfile)
                .preference(Map.of())
                .rolePlayEnabled(role != null)
                .companionRole(roleMap)
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
        userProfileService.recordActivity(
                conversation.getSpaceId(), request.getSubject(), List.of(), List.of(), "chat",
                JsonUtils.toJson(Map.of(
                        "user_message", shorten(request.getMessage(), 1200),
                        "assistant_reply", shorten(aiResponse.getReplyMarkdown(), 1200),
                        "subject", valueOrBlank(request.getSubject())
                )));

        return ChatResponseVO.builder()
                .conversation(toConversationVO(conversation, role))
                .userMessage(toMessageVO(userMessage))
                .assistantMessage(toMessageVO(assistantMessage))
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConversationVO applyRole(Long conversationId, ConversationRoleRequest request) {
        Conversation conversation = getOwnedConversation(conversationId);
        boolean enabled = request != null && Boolean.TRUE.equals(request.getRolePlayEnabled());
        Long roleId = request == null ? null : request.getRoleId();
        CompanionRole role = null;
        if (enabled) {
            if (roleId == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "开启角色扮演前需要先选择 AI 角色");
            }
            role = companionRoleService.getOwnedActiveRole(roleId);
            conversation.setRoleId(role.getId());
            conversation.setRolePlayEnabled(true);
        } else {
            if (roleId != null) {
                role = companionRoleService.getOwnedActiveRole(roleId);
                conversation.setRoleId(role.getId());
            }
            conversation.setRolePlayEnabled(false);
        }
        conversationMapper.updateById(conversation);
        return toConversationVO(conversation, role);
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

    private CompanionRole resolveActiveRole(Conversation conversation) {
        if (!Boolean.TRUE.equals(conversation.getRolePlayEnabled()) || conversation.getRoleId() == null) {
            return null;
        }
        return companionRoleService.getOwnedActiveRole(conversation.getRoleId());
    }

    private ConversationVO toConversationVO(Conversation entity, CompanionRole role) {
        return ConversationVO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .spaceId(entity.getSpaceId())
                .title(entity.getTitle())
                .intentType(entity.getIntentType())
                .summary(entity.getSummary())
                .roleId(entity.getRoleId())
                .rolePlayEnabled(Boolean.TRUE.equals(entity.getRolePlayEnabled()))
                .roleName(role == null ? null : role.getRoleName())
                .roleIdentity(role == null ? null : role.getRoleIdentity())
                .roleThemeColor(role == null ? null : role.getThemeColor())
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

    private Map<String, Object> roleToMap(CompanionRole role) {
        if (role == null) {
            return Map.of();
        }
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("role_id", role.getId());
        values.put("role_name", valueOrBlank(role.getRoleName()));
        values.put("role_identity", valueOrBlank(role.getRoleIdentity()));
        values.put("background", valueOrBlank(role.getBackground()));
        values.put("personality", valueOrBlank(role.getPersonality()));
        values.put("expertise", valueOrBlank(role.getExpertise()));
        values.put("hobbies", valueOrBlank(role.getHobbies()));
        values.put("speaking_style", valueOrBlank(role.getSpeakingStyle()));
        values.put("scenario", valueOrBlank(role.getScenario()));
        values.put("companion_goal", valueOrBlank(role.getCompanionGoal()));
        values.put("boundaries", valueOrBlank(role.getBoundaries()));
        values.put("custom_prompt", valueOrBlank(role.getCustomPrompt()));
        values.put("tags", valueOrBlank(role.getTags()));
        return values;
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
