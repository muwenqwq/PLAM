package com.learnagent.client.dto;

import lombok.Data;
import java.util.List;

@Data
public class AiStudyPlanResponse {
    private List<NodeItem> nodes;
    private List<AgentTrace> agentTrace;

    @Data
    public static class NodeItem {
        private Integer order;
        private Long knowledgePointId;
        private String knowledgePoint;
        private List<Long> recommendedResourceIds;
        private Integer estimatedMinutes;
        private String reason;
        private String completionCriteria;
    }
}
