package com.edustudio.integration.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AiQuizGenerateResponse {

    private Boolean success;

    private String title;

    private String subject;

    private String difficulty;

    @JsonProperty("total_score")
    private BigDecimal totalScore;

    private List<AiQuizQuestionDTO> questions;
}
