package com.edustudio.module.resource.controller;

import com.edustudio.common.api.PageResult;
import com.edustudio.common.api.Result;
import com.edustudio.module.resource.dto.GeneratedResourceQueryRequest;
import com.edustudio.module.resource.service.GeneratedResourceService;
import com.edustudio.module.resource.vo.GeneratedResourceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "生成资源")
@Validated
@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class GeneratedResourceController {

    private final GeneratedResourceService generatedResourceService;

    @Operation(summary = "分页查询生成资源")
    @GetMapping
    public Result<PageResult<GeneratedResourceVO>> page(@Valid GeneratedResourceQueryRequest request) {
        return Result.success(generatedResourceService.page(request));
    }

    @Operation(summary = "查询生成资源详情")
    @GetMapping("/{id}")
    public Result<GeneratedResourceVO> detail(@PathVariable Long id) {
        return Result.success(generatedResourceService.detail(id));
    }
}
