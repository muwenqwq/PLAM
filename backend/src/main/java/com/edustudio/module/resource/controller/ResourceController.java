package com.edustudio.module.resource.controller;

import com.edustudio.common.api.PageResult;
import com.edustudio.common.api.Result;
import com.edustudio.module.resource.dto.ResourceGenerateRequest;
import com.edustudio.module.resource.dto.ResourceQueryRequest;
import com.edustudio.module.resource.dto.ResourceUpdateRequest;
import com.edustudio.module.resource.service.ResourceService;
import com.edustudio.module.resource.vo.GeneratedResourceVO;
import com.edustudio.module.resource.vo.ResourceGenerateResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "资源生成中心")
@Validated
@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @Operation(summary = "生成资源")
    @PostMapping("/generate")
    public Result<ResourceGenerateResultVO> generate(@Valid @RequestBody ResourceGenerateRequest request) {
        return Result.success(resourceService.generate(request));
    }

    @Operation(summary = "分页查询资源")
    @GetMapping
    public Result<PageResult<GeneratedResourceVO>> page(@Valid ResourceQueryRequest request) {
        return Result.success(resourceService.page(request));
    }

    @Operation(summary = "查询资源详情")
    @GetMapping("/{id}")
    public Result<GeneratedResourceVO> detail(@PathVariable Long id) {
        return Result.success(resourceService.detail(id));
    }

    @Operation(summary = "更新资源")
    @PutMapping("/{id}")
    public Result<GeneratedResourceVO> update(@PathVariable Long id, @Valid @RequestBody ResourceUpdateRequest request) {
        return Result.success(resourceService.update(id, request));
    }

    @Operation(summary = "删除资源")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        resourceService.delete(id);
        return Result.success();
    }

    @Operation(summary = "导出 Markdown")
    @PostMapping("/{id}/export/markdown")
    public Result<String> exportMarkdown(@PathVariable Long id) {
        return Result.success(resourceService.exportMarkdown(id));
    }

    @Operation(summary = "基于资源生成 Mermaid 知识图谱")
    @PostMapping("/{id}/graph")
    public Result<GeneratedResourceVO> generateGraph(@PathVariable Long id) {
        return Result.success(resourceService.generateGraph(id));
    }
}
