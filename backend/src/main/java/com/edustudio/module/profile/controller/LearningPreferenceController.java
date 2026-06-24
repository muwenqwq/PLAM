package com.edustudio.module.profile.controller;

import com.edustudio.common.api.Result;
import com.edustudio.module.profile.dto.LearningPreferenceUpsertRequest;
import com.edustudio.module.profile.service.LearningPreferenceService;
import com.edustudio.module.profile.vo.LearningPreferenceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "学习偏好")
@Validated
@RestController
@RequestMapping("/api/preferences")
@RequiredArgsConstructor
public class LearningPreferenceController {

    private final LearningPreferenceService learningPreferenceService;

    @Operation(summary = "查询当前用户学习偏好")
    @GetMapping("/me")
    public Result<LearningPreferenceVO> me() {
        return Result.success(learningPreferenceService.getMe());
    }

    @Operation(summary = "创建或更新当前用户学习偏好")
    @PutMapping("/me")
    public Result<LearningPreferenceVO> upsertMe(@Valid @RequestBody LearningPreferenceUpsertRequest request) {
        return Result.success(learningPreferenceService.upsertMe(request));
    }
}
