package com.edustudio.module.modelprovider.controller;

import com.edustudio.common.api.PageResult;
import com.edustudio.common.api.Result;
import com.edustudio.module.modelprovider.dto.ModelProviderCreateRequest;
import com.edustudio.module.modelprovider.dto.ModelProviderQueryRequest;
import com.edustudio.module.modelprovider.dto.ModelProviderTestRequest;
import com.edustudio.module.modelprovider.dto.ModelProviderUpdateRequest;
import com.edustudio.module.modelprovider.service.ModelProviderService;
import com.edustudio.module.modelprovider.vo.ModelProviderTestVO;
import com.edustudio.module.modelprovider.vo.ModelProviderVO;
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

@Tag(name = "AI 模型配置")
@Validated
@RestController
@RequestMapping("/api/model-providers")
@RequiredArgsConstructor
public class ModelProviderController {

    private final ModelProviderService modelProviderService;

    @Operation(summary = "创建模型配置")
    @PostMapping
    public Result<ModelProviderVO> create(@Valid @RequestBody ModelProviderCreateRequest request) {
        return Result.success(modelProviderService.create(request));
    }

    @Operation(summary = "分页查询模型配置")
    @GetMapping
    public Result<PageResult<ModelProviderVO>> page(@Valid ModelProviderQueryRequest request) {
        return Result.success(modelProviderService.page(request));
    }

    @Operation(summary = "查询默认模型配置")
    @GetMapping("/default")
    public Result<ModelProviderVO> getDefault() {
        return Result.success(modelProviderService.getDefault());
    }

    @Operation(summary = "查询模型配置详情")
    @GetMapping("/{id}")
    public Result<ModelProviderVO> detail(@PathVariable Long id) {
        return Result.success(modelProviderService.detail(id));
    }

    @Operation(summary = "更新模型配置")
    @PutMapping("/{id}")
    public Result<ModelProviderVO> update(
            @PathVariable Long id,
            @Valid @RequestBody ModelProviderUpdateRequest request
    ) {
        return Result.success(modelProviderService.update(id, request));
    }

    @Operation(summary = "删除模型配置")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        modelProviderService.delete(id);
        return Result.success();
    }

    @Operation(summary = "设置默认模型配置")
    @PostMapping("/{id}/default")
    public Result<ModelProviderVO> setDefault(@PathVariable Long id) {
        return Result.success(modelProviderService.setDefault(id));
    }

    @Operation(summary = "测试模型连接")
    @PostMapping("/{id}/test")
    public Result<ModelProviderTestVO> test(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ModelProviderTestRequest request
    ) {
        return Result.success(modelProviderService.test(id, request == null ? new ModelProviderTestRequest() : request));
    }
}
