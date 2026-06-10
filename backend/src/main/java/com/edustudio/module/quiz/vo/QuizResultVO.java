package com.edustudio.module.quiz.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class QuizResultVO {

    private Long quizId;
    private BigDecimal score;
    private BigDecimal totalScore;
    private BigDecimal accuracyRate;
    private List<String> weakPoints;
    private String analysisMarkdown;
}
