package com.edustudio.module.learningpath.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.List;

@Data
public class LearningPathGenerateRequest {

    @NotNull(message = "学习空间不能为空")
    private Long spaceId;

    private Long modelProviderId;

    @NotBlank(message = "学科不能为空")
    private String subject;

    @NotBlank(message = "学习目标不能为空")
    private String goal;

    private List<String> knowledgePoints;

    @Min(value = 1, message = "学习天数不能小于 1")
    @Max(value = 90, message = "学习天数不能超过 90")
    private Integer days = 7;

    private JsonNode preference;
}
