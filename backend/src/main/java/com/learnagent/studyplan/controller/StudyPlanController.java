package com.learnagent.studyplan.controller;

import com.learnagent.studyplan.dto.StudyPlanGenerateRequest;
import com.learnagent.studyplan.dto.StudyPlanResponse;
import com.learnagent.studyplan.service.StudyPlanService;
import com.learnagent.common.dto.ApiResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/study-plan")
@RequiredArgsConstructor
public class StudyPlanController {

    private final StudyPlanService studyPlanService;

    @PostMapping("/generate")
    public ApiResult<StudyPlanResponse> generate(@Valid @RequestBody StudyPlanGenerateRequest req) {
        return ApiResult.ok(studyPlanService.generateStudyPlan(req));
    }

    @GetMapping("/{studentId}")
    public ApiResult<StudyPlanResponse> get(@PathVariable Long studentId) {
        return ApiResult.ok(studyPlanService.getStudyPlan(studentId));
    }
}
