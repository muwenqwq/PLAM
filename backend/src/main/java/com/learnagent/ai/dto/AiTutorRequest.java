package com.learnagent.ai.dto;

import lombok.Data;
import java.util.Map;

@Data
public class AiTutorRequest {
    private String taskId;
    private Long studentId;
    private Long courseId;
    private String question;
    private Map<String, Object> profileJson;
}
