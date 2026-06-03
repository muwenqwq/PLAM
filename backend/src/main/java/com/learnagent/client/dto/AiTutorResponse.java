package com.learnagent.client.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class AiTutorResponse {
    private String answer;
    private List<Map<String, String>> sources;
    private List<Long> suggestedResourceIds;
    private String safetyStatus;
    private List<AgentTrace> agentTrace;
}
