package com.learnagent.ai.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class AiStudyPlanRequest {
    private String taskId;
    private Long studentId;
    private Long courseId;
    private Map<String, Object> profileJson;
    private List<KpInfo> knowledgePoints;
    private List<DepInfo> dependencies;
    private List<ResInfo> resources;

    @Data
    public static class KpInfo {
        private Long id;
        private String name;
        private Integer difficulty;
    }

    @Data
    public static class DepInfo {
        private Long from;
        private Long to;
        private String relation;
    }

    @Data
    public static class ResInfo {
        private Long id;
        private String type;
        private Long knowledgePointId;
    }
}
