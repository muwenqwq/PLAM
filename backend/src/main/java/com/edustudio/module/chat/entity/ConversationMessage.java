package com.edustudio.module.chat.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.edustudio.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("conversation_message")
public class ConversationMessage extends BaseEntity {

    private Long conversationId;

    private Long userId;

    private String messageRole;

    private String contentMd;

    private String contentJson;

    private Integer tokenCount;

    private String status;
}
