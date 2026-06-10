package com.edustudio.module.report.controller;

import com.edustudio.common.api.Result;
import com.edustudio.module.report.dto.ReportGenerateRequest;
import com.edustudio.module.report.service.ReportService;
import com.edustudio.module.report.vo.LearningReportVO;
import com.edustudio.module.report.vo.ReportOverviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "学习报告")
@Validated
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "学习概览")
    @GetMapping("/overview")
    public Result<ReportOverviewVO> overview() {
        return Result.success(reportService.overview());
    }

    @Operation(summary = "查询空间报告")
    @GetMapping("/space/{spaceId}")
    public Result<List<LearningReportVO>> listBySpace(@PathVariable Long spaceId) {
        return Result.success(reportService.listBySpace(spaceId));
    }

    @Operation(summary = "生成学习报告")
    @PostMapping("/generate")
    public Result<LearningReportVO> generate(@Valid @RequestBody ReportGenerateRequest request) {
        return Result.success(reportService.generate(request));
    }

    @Operation(summary = "查询报告详情")
    @GetMapping("/{id}")
    public Result<LearningReportVO> detail(@PathVariable Long id) {
        return Result.success(reportService.detail(id));
    }

    @Operation(summary = "导出报告 Markdown")
    @PostMapping("/{id}/export")
    public Result<String> export(@PathVariable Long id) {
        return Result.success(reportService.exportMarkdown(id));
    }
}
