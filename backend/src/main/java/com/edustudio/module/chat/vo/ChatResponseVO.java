package com.edustudio.module.chat.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponseVO {

    private ConversationVO conversation;

    private ConversationMessageVO userMessage;

    private ConversationMessageVO assistantMessage;
}
