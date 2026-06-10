package com.edustudio.module.chat.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMessageVO {

    private Long id;

    private Long conversationId;

    private Long userId;

    private String messageRole;

    private String contentMd;

    private JsonNode contentJson;

    private Integer tokenCount;

    private String status;

    private LocalDateTime createdAt;
}
