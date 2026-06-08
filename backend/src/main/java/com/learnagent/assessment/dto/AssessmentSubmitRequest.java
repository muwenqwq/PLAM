package com.learnagent.assessment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class AssessmentSubmitRequest {
    @NotNull private Long studentId;
    @NotNull private Long courseId;
    @NotNull private Long knowledgePointId;
    private Long resourceId;
    private List<AnswerItem> answers;

    @Data
    public static class AnswerItem {
        private Long questionId;
        private String answer;
        private Boolean correct;
    }
}
