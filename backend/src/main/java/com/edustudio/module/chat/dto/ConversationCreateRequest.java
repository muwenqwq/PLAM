package com.edustudio.module.chat.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConversationCreateRequest {

    @NotNull(message = "学习空间不能为空")
    private Long spaceId;

    @Size(max = 200, message = "会话标题不能超过 200 个字符")
    private String title;

    @Size(max = 64, message = "意图类型不能超过 64 个字符")
    private String intentType;

    private Long roleId;

    private Boolean rolePlayEnabled;
}
