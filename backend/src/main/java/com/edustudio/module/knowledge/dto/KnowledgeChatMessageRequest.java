package com.edustudio.module.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class KnowledgeChatMessageRequest {

    @Pattern(regexp = "user|assistant", message = "历史消息角色只能是 user 或 assistant")
    private String role;

    @NotBlank(message = "历史消息内容不能为空")
    @Size(max = 12000, message = "单条历史消息不能超过 12000 个字符")
    private String content;
}
