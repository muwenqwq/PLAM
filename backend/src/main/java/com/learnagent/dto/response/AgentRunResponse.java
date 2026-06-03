package com.learnagent.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AgentRunResponse {
    private Long id;
    private String taskId;
    private String agentName;
    private String inputSummary;
    private String outputSummary;
    private String modelName;
    private String status;
    private Integer latencyMs;
    private String errorMessage;
    private LocalDateTime createdAt;
}
