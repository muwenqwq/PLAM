package com.learnagent.studyplan.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class StudyPlanResponse {
    private Long planId;
    private Long studentId;
    private Long courseId;
    private LocalDateTime createdAt;
    private List<NodeData> nodes;

    @Data
    public static class NodeData {
        private Long id;
        private Integer order;
        private String knowledgePoint;
        private List<Long> recommendedResourceIds;
        private String estimatedDuration;
        private String reason;
        private String completionCriteria;
        private String status;
    }
}
