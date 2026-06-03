package com.learnagent.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StudyPlanGenerateRequest {
    @NotNull private Long studentId;
    @NotNull private Long courseId;
}
