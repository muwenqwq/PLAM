package com.edustudio.module.agent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.edustudio.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_task")
public class AgentTask extends BaseEntity {

    private Long userId;

    private Long spaceId;

    private Long providerId;

    private String taskType;

    private String title;

    private String inputParams;

    private String executionStatus;

    private String outputSummary;

    private String resultJson;

    private String errorMessage;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private String status;
}
