package com.learnagent.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ResourceResponse {
    private Long id;
    private String resourceType;
    private String title;
    private String format;
    private BigDecimal qualityScore;
    private String status;
    private LocalDateTime createdAt;
    private String content;
}
