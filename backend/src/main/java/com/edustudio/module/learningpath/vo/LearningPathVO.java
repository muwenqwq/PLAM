package com.edustudio.module.learningpath.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class LearningPathVO {

    private Long id;
    private Long userId;
    private Long spaceId;
    private String title;
    private String goal;
    private String subject;
    private JsonNode planJson;
    private BigDecimal progressRate;
    private LocalDate startDate;
    private LocalDate targetDate;
    private String status;
    private LocalDateTime createdAt;
    private List<LearningPathItemVO> items;
}
