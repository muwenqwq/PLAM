package com.edustudio.module.learningspace.controller;

import com.edustudio.common.api.PageResult;
import com.edustudio.common.api.Result;
import com.edustudio.module.learningspace.dto.LearningSpaceCreateRequest;
import com.edustudio.module.learningspace.dto.LearningSpaceQueryRequest;
import com.edustudio.module.learningspace.dto.LearningSpaceUpdateRequest;
import com.edustudio.module.learningspace.service.LearningSpaceService;
import com.edustudio.module.learningspace.vo.LearningSpaceSummaryVO;
import com.edustudio.module.learningspace.vo.LearningSpaceVO;
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

@Tag(name = "学习空间")
@Validated
@RestController
@RequestMapping("/api/learning-spaces")
@RequiredArgsConstructor
public class LearningSpaceController {

    private final LearningSpaceService learningSpaceService;

    @Operation(summary = "创建学习空间")
    @PostMapping
    public Result<LearningSpaceVO> create(@Valid @RequestBody LearningSpaceCreateRequest request) {
        return Result.success(learningSpaceService.create(request));
    }

    @Operation(summary = "分页查询当前用户学习空间")
    @GetMapping
    public Result<PageResult<LearningSpaceVO>> page(@Valid LearningSpaceQueryRequest request) {
        return Result.success(learningSpaceService.page(request));
    }

    @Operation(summary = "查询当前用户默认学习空间")
    @GetMapping("/default")
    public Result<LearningSpaceVO> getDefault() {
        return Result.success(learningSpaceService.getDefault());
    }

    @Operation(summary = "查询学习空间详情")
    @GetMapping("/{id}")
    public Result<LearningSpaceVO> detail(@PathVariable Long id) {
        return Result.success(learningSpaceService.detail(id));
    }

    @Operation(summary = "更新学习空间")
    @PutMapping("/{id}")
    public Result<LearningSpaceVO> update(
            @PathVariable Long id,
            @Valid @RequestBody LearningSpaceUpdateRequest request
    ) {
        return Result.success(learningSpaceService.update(id, request));
    }

    @Operation(summary = "删除学习空间")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        learningSpaceService.delete(id);
        return Result.success();
    }

    @Operation(summary = "设置默认学习空间")
    @PostMapping("/{id}/default")
    public Result<LearningSpaceVO> setDefault(@PathVariable Long id) {
        return Result.success(learningSpaceService.setDefault(id));
    }

    @Operation(summary = "查询学习空间统计摘要")
    @GetMapping("/{id}/summary")
    public Result<LearningSpaceSummaryVO> summary(@PathVariable Long id) {
        return Result.success(learningSpaceService.summary(id));
    }
}
