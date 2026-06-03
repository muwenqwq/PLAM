package com.learnagent.controller;

import com.learnagent.dto.response.AnalyticsResponse;
import com.learnagent.dto.response.ApiResult;
import com.learnagent.service.AssessmentService;
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
