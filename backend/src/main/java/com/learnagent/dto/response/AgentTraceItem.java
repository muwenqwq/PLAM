package com.learnagent.dto.response;

import lombok.Data;

@Data
public class AgentTraceItem {
    private String agentName;
    private String status;
    private String inputSummary;
    private String outputSummary;
    private String modelName;
    private Integer latencyMs;
}
