package com.edustudio.module.quiz.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.edustudio.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("quiz_answer")
public class QuizAnswer extends BaseEntity {

    private Long quizId;
    private Long questionId;
    private Long userId;
    private String answerText;
    private BigDecimal score;
    private Boolean isCorrect;
    private String feedbackText;
    private LocalDateTime submittedAt;
    private String status;
}
