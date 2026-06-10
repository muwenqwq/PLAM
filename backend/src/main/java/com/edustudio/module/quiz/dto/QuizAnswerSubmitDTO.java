package com.edustudio.module.quiz.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QuizAnswerSubmitDTO {

    @NotNull(message = "题目不能为空")
    private Long questionId;

    private String answerText;
}
