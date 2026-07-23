package com.edustudio.module.quiz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class QuizSubmitRequest {

    @Valid
    @NotEmpty(message = "答案不能为空")
    private List<QuizAnswerSubmitDTO> answers;

    private Boolean rolePlayEnabled = false;

    private Long companionRoleId;
}