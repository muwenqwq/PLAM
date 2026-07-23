package com.edustudio.module.report.controller;

import com.edustudio.common.api.Result;
import com.edustudio.module.report.dto.ReportGenerateRequest;
import com.edustudio.module.report.service.ReportService;
import com.edustudio.module.report.vo.LearningReportVO;
import com.edustudio.module.report.vo.ReportOverviewVO;
import com.edustudio.module.resource.export.DocumentExportService;
import com.edustudio.module.resource.export.ExportedDocument;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Tag(name = "学习报告")
@Validated
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final DocumentExportService documentExportService;

    @Operation(summary = "学习概览")
    @GetMapping("/overview")
    public Result<ReportOverviewVO> overview(@RequestParam(required = false) Long spaceId) {
        return Result.success(reportService.overview(spaceId));
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

    @Operation(summary = "删除学习报告")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        reportService.delete(id);
        return Result.success();
    }

    @Operation(summary = "导出报告 Markdown")
    @PostMapping("/{id}/export")
    public Result<String> export(@PathVariable Long id) {
        return Result.success(reportService.exportMarkdown(id));
    }

    @Operation(summary = "下载报告（Word、PDF、PNG 或 Markdown）")
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id, @RequestParam(defaultValue = "docx") String format) {
        LearningReportVO report = reportService.detail(id);
        String markdown = reportService.exportMarkdown(id);
        ExportedDocument exported = documentExportService.export(report.getTitle(), markdown, format);
        String filename = filename(report.getTitle(), "report-" + id, exported.extension());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(exported.mediaType()))
                .contentLength(exported.bytes().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(filename, exported.extension()))
                .body(exported.bytes());
    }

    private String filename(String title, String fallback, String format) {
        String extension = format.toLowerCase();
        String base = (title == null || title.isBlank() ? fallback : title)
                .replaceAll("[\\\\/:*?\"<>|]+", "_")
                .replaceAll("\\s+", " ")
                .trim();
        if (base.isBlank()) {
            base = fallback;
        }
        return base.endsWith("." + extension) ? base : base + "." + extension;
    }

    private String contentDisposition(String filename, String extension) {
        String encoded = UriUtils.encode(filename, StandardCharsets.UTF_8);
        return "attachment; filename=\"download." + extension + "\"; filename*=UTF-8''" + encoded;
    }
}
