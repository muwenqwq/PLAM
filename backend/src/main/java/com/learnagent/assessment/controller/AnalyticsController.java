package com.learnagent.assessment.controller;

import com.learnagent.assessment.dto.AnalyticsResponse;
import com.learnagent.assessment.service.AssessmentService;
import com.learnagent.common.dto.ApiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AssessmentService assessmentService;

    @GetMapping("/{studentId}")
    public ApiResult<AnalyticsResponse> get(@PathVariable Long studentId) {
        return ApiResult.ok(assessmentService.getAnalytics(studentId));
    }
}
