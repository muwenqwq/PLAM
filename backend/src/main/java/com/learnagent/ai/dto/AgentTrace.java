package com.learnagent.ai.dto;

import lombok.Data;

@Data
public class AgentTrace {
    private String agentName;
    private String status;
    private String inputSummary;
    private String outputSummary;
    private String modelName;
    private Integer latencyMs;
}
