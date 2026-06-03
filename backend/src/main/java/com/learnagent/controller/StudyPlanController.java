package com.learnagent.controller;

import com.learnagent.dto.request.StudyPlanGenerateRequest;
import com.learnagent.dto.response.ApiResult;
import com.learnagent.dto.response.StudyPlanResponse;
import com.learnagent.service.StudyPlanService;
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
