package com.edustudio.module.agent.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentStepVO {

    private Long id;
    private Long taskId;
    private Long userId;
    private String agentName;
    private Integer stepOrder;
    private String stepType;
    private String executionStatus;
    private JsonNode inputJson;
    private String outputSummary;
    private JsonNode resultJson;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String status;
}
