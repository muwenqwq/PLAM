package com.edustudio.module.learningpath.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class LearningPathUpdateRequest {

    @NotBlank(message = "路径标题不能为空")
    @Size(max = 200, message = "路径标题不能超过 200 个字符")
    private String title;

    @NotBlank(message = "学习目标不能为空")
    @Size(max = 1000, message = "学习目标不能超过 1000 个字符")
    private String goal;

    @NotBlank(message = "学科不能为空")
    @Size(max = 100, message = "学科不能超过 100 个字符")
    private String subject;

    private LocalDate startDate;

    private LocalDate targetDate;

    @Valid
    @NotEmpty(message = "学习路径至少需要一个任务")
    @Size(max = 100, message = "学习路径最多包含 100 个任务")
    private List<LearningPathItemEditRequest> items = new ArrayList<>();
}
