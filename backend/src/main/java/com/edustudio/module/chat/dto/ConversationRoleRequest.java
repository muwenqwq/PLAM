package com.edustudio.module.chat.dto;

import lombok.Data;

@Data
public class ConversationRoleRequest {

    private Long roleId;

    private Boolean rolePlayEnabled;
}
