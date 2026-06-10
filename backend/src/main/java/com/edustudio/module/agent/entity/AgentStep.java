package com.edustudio.module.agent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.edustudio.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_step")
public class AgentStep extends BaseEntity {

    private Long taskId;

    private Long userId;

    private String agentName;

    private Integer stepOrder;

    private String stepType;

    private String executionStatus;

    private String inputJson;

    private String outputSummary;

    private String resultJson;

    private String errorMessage;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private String status;
}
