package com.edustudio.module.resource.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedResourceVO {

    private Long id;

    private Long userId;

    private Long spaceId;

    private Long taskId;

    private String resourceType;

    private String title;

    private String subject;

    private JsonNode knowledgePoints;

    private String contentMarkdown;

    private JsonNode contentJson;

    private String outputSummary;

    private BigDecimal qualityScore;

    private String exportStatus;

    private String status;

    private LocalDateTime createdAt;
}
