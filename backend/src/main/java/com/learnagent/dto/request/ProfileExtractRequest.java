package com.learnagent.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProfileExtractRequest {
    @NotNull(message = "studentId 不能为空")
    private Long studentId;
    @NotNull(message = "courseId 不能为空")
    private Long courseId;
    @NotBlank(message = "studentMessage 不能为空")
    private String studentMessage;
}
