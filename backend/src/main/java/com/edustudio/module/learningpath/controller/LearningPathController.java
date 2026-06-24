package com.edustudio.module.learningpath.controller;

import com.edustudio.common.api.PageResult;
import com.edustudio.common.api.Result;
import com.edustudio.module.learningpath.dto.LearningPathGenerateRequest;
import com.edustudio.module.learningpath.dto.LearningPathItemStatusRequest;
import com.edustudio.module.learningpath.dto.LearningPathQueryRequest;
import com.edustudio.module.learningpath.service.LearningPathService;
import com.edustudio.module.learningpath.vo.LearningPathItemVO;
import com.edustudio.module.learningpath.vo.LearningPathVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "学习路径")
@Validated
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LearningPathController {

    private final LearningPathService learningPathService;

    @Operation(summary = "生成学习路径")
    @PostMapping("/learning-paths/generate")
    public Result<LearningPathVO> generate(@Valid @RequestBody LearningPathGenerateRequest request) {
        return Result.success(learningPathService.generate(request));
    }

    @Operation(summary = "分页查询学习路径")
    @GetMapping("/learning-paths")
    public Result<PageResult<LearningPathVO>> page(@Valid LearningPathQueryRequest request) {
        return Result.success(learningPathService.page(request));
    }

    @Operation(summary = "查询学习路径详情")
    @GetMapping("/learning-paths/{id}")
    public Result<LearningPathVO> detail(@PathVariable Long id) {
        return Result.success(learningPathService.detail(id));
    }

    @Operation(summary = "更新路径任务状态")
    @PutMapping("/learning-path-items/{id}/status")
    public Result<LearningPathItemVO> updateItemStatus(@PathVariable Long id, @Valid @RequestBody LearningPathItemStatusRequest request) {
        return Result.success(learningPathService.updateItemStatus(id, request));
    }

    @Operation(summary = "查询今日学习任务")
    @GetMapping("/learning-paths/today")
    public Result<List<LearningPathItemVO>> today() {
        return Result.success(learningPathService.today());
    }

    @Operation(summary = "调整学习路径")
    @PostMapping("/learning-paths/{id}/adjust")
    public Result<LearningPathVO> adjust(@PathVariable Long id) {
        return Result.success(learningPathService.adjust(id));
    }
}
