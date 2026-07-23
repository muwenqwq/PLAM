package com.edustudio.module.learningpath.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class LearningPathItemEditRequest {

    private Long id;

    @NotBlank(message = "任务标题不能为空")
    @Size(max = 200, message = "任务标题不能超过 200 个字符")
    private String title;

    @Size(max = 4000, message = "任务说明不能超过 4000 个字符")
    private String description;

    private JsonNode knowledgePoints;

    @Min(value = 1, message = "预计时长不能小于 1 分钟")
    @Max(value = 1440, message = "预计时长不能超过 1440 分钟")
    private Integer estimatedMinutes;

    private LocalDate dueDate;

    @Pattern(regexp = "todo|doing|done", message = "任务状态只能是 todo、doing 或 done")
    private String status;
}
