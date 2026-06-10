package com.edustudio.module.chat.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationVO {

    private Long id;

    private Long userId;

    private Long spaceId;

    private String title;

    private String intentType;

    private String summary;

    private Integer messageCount;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
