package com.edustudio.module.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReportGenerateRequest {

    @NotNull(message = "学习空间不能为空")
    private Long spaceId;

    private Long modelProviderId;

    private String reportType = "space_weekly";

    @NotBlank(message = "报告标题不能为空")
    private String title;
}
