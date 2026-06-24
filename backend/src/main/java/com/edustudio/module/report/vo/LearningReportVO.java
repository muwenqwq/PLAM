package com.edustudio.module.report.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LearningReportVO {

    private Long id;
    private Long userId;
    private Long spaceId;
    private String reportType;
    private String title;
    private String summary;
    private JsonNode reportJson;
    private JsonNode chartDataJson;
    private String suggestionText;
    private String status;
    private LocalDateTime createdAt;
}
