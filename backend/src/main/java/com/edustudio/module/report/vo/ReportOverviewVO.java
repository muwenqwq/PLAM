package com.edustudio.module.report.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ReportOverviewVO {

    private Long spaceCount;
    private Long resourceCount;
    private Long quizCount;
    private Long pathCount;
    private BigDecimal averageScore;
    private BigDecimal averageMastery;
}
