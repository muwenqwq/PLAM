package com.edustudio.module.quiz.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class QuizQuestionFeedbackVO {

    private Long questionId;
    private Integer questionOrder;
    private String stem;
    private List<String> options;
    private String answerText;
    private String correctAnswer;
    private BigDecimal score;
    private BigDecimal fullScore;
    private Boolean correct;
    private List<String> knowledgePoints;
    private String feedback;
    private Map<String, String> optionExplanations;
}
