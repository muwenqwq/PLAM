package com.learnagent.client.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class AiAssessmentResponse {
    private Double score;
    private Double totalScore;
    private Map<String, Double> masteryDelta;
    private List<Map<String, Object>> details;
    private List<String> weakPointsIdentified;
    private List<AgentTrace> agentTrace;
}
