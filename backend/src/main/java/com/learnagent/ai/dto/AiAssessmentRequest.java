package com.learnagent.ai.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class AiAssessmentRequest {
    private String taskId;
    private Long studentId;
    private Long courseId;
    private Long knowledgePointId;
    private List<Map<String, Object>> answers;
    private Map<String, Double> currentMastery;
}
