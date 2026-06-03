package com.learnagent.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TutorAskRequest {
    @NotNull private Long studentId;
    @NotNull private Long courseId;
    @NotBlank private String question;
}
