package com.edustudio.module.quiz.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class QuizQuestionVO {

    private Long id;
    private Integer questionOrder;
    private String questionType;
    private String stem;
    private JsonNode options;
    private String answerText;
    private String analysisText;
    private JsonNode knowledgePoints;
    private String difficulty;
    private BigDecimal score;
}
