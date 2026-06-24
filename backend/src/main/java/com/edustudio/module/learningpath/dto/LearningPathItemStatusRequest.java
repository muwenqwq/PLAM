package com.edustudio.module.learningpath.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class LearningPathItemStatusRequest {

    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "todo|doing|done|skipped", message = "状态只能为 todo、doing、done、skipped")
    private String status;
}
