package com.learnagent.ai.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class AiResourceResponse {
    private List<ResourceItem> resources;
    private ReviewResult reviewResult;
    private List<AgentTrace> agentTrace;

    @Data
    public static class ResourceItem {
        private String resourceType;
        private String title;
        private String content;
        private String format;
        private List<Map<String, String>> sourcesJson;
    }

    @Data
    public static class ReviewResult {
        private Double averageScore;
        private List<ReviewDetail> details;

        @Data
        public static class ReviewDetail {
            private String resourceType;
            private Double qualityScore;
            private List<String> issues;
            private List<String> suggestions;
        }
    }
}
