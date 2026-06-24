package com.edustudio.module.quiz.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class QuizVO {

    private Long id;
    private Long userId;
    private Long spaceId;
    private Long resourceId;
    private String title;
    private String subject;
    private String difficulty;
    private Integer questionCount;
    private BigDecimal totalScore;
    private String status;
    private LocalDateTime createdAt;
    private List<QuizQuestionVO> questions;
}
