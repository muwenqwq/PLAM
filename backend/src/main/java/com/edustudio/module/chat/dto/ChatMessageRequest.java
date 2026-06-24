package com.edustudio.module.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatMessageRequest {

    private Long modelProviderId;

    @Size(max = 128, message = "学科不能超过 128 个字符")
    private String subject;

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 5000, message = "消息内容不能超过 5000 个字符")
    private String message;
}
