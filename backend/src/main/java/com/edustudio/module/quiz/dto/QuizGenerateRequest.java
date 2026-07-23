package com.edustudio.module.quiz.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class QuizGenerateRequest {

    @NotNull(message = "学习空间不能为空")
    private Long spaceId;

    private Long resourceId;
    private Long modelProviderId;

    @NotBlank(message = "学科不能为空")
    private String subject;

    private String title;
    private List<String> knowledgePoints;

    @Min(value = 1, message = "题目数量不能小于 1")
    @Max(value = 30, message = "题目数量不能超过 30")
    private Integer questionCount = 5;

    private String difficulty = "medium";

    private String questionType = "single_choice";

    private Boolean rolePlayEnabled = false;

    private Long companionRoleId;
}
