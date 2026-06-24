package com.edustudio.module.chat.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConversationCreateRequest {

    private Long spaceId;

    @Size(max = 200, message = "会话标题不能超过 200 个字符")
    private String title;

    @Size(max = 64, message = "意图类型不能超过 64 个字符")
    private String intentType;
}
