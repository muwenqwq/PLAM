package com.edustudio.module.learningpath.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class LearningPathItemVO {

    private Long id;
    private Long pathId;
    private Integer itemOrder;
    private String title;
    private String description;
    private Long resourceId;
    private JsonNode knowledgePoints;
    private Integer estimatedMinutes;
    private LocalDate dueDate;
    private LocalDateTime completedAt;
    private String status;
}
