package com.edustudio.integration.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class AiQuizQuestionDTO {

    @JsonProperty("question_order")
    private Integer questionOrder;

    @JsonProperty("question_type")
    private String questionType;

    private String stem;

    private List<String> options;

    @JsonProperty("answer_text")
    private String answerText;

    @JsonProperty("analysis_text")
    private String analysisText;

    @JsonProperty("option_explanations")
    private Map<String, String> optionExplanations;

    @JsonProperty("knowledge_points")
    private List<String> knowledgePoints;

    private String difficulty;

    private BigDecimal score;
}
