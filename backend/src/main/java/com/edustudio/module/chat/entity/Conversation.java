package com.edustudio.module.chat.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.edustudio.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("conversation")
public class Conversation extends BaseEntity {

    private Long userId;

    private Long spaceId;

    private String title;

    private String intentType;

    private String summary;

    private Integer messageCount;

    private String status;
}
