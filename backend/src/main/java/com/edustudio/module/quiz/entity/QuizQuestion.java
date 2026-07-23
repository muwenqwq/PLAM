package com.edustudio.module.quiz.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.edustudio.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("quiz_question")
public class QuizQuestion extends BaseEntity {

    private Long quizId;
    private Long userId;
    private Integer questionOrder;
    private String questionType;
    private String stem;
    private String optionsJson;
    private String answerText;
    private String analysisText;
    private String optionAnalysisJson;
    private String knowledgePoints;
    private String difficulty;
    private BigDecimal score;
    private String status;
}
