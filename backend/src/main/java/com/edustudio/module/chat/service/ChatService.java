package com.edustudio.module.chat.service;

import com.edustudio.common.api.PageResult;
import com.edustudio.module.chat.dto.ChatMessageRequest;
import com.edustudio.module.chat.dto.ConversationCreateRequest;
import com.edustudio.module.chat.dto.ConversationQueryRequest;
import com.edustudio.module.chat.dto.ConversationRoleRequest;
import com.edustudio.module.chat.vo.ChatResponseVO;
import com.edustudio.module.chat.vo.ConversationMessageVO;
import com.edustudio.module.chat.vo.ConversationVO;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.function.Consumer;

public interface ChatService {

    ConversationVO createConversation(ConversationCreateRequest request);

    PageResult<ConversationVO> page(ConversationQueryRequest request);

    ConversationVO detail(Long id);

    List<ConversationMessageVO> messages(Long conversationId);

    ChatResponseVO sendMessage(Long conversationId, ChatMessageRequest request);

    ChatResponseVO streamMessage(Long conversationId, ChatMessageRequest request, Consumer<String> onDelta);

    ConversationVO applyRole(Long conversationId, ConversationRoleRequest request);

    JsonNode detectIntent(ChatMessageRequest request);
}
