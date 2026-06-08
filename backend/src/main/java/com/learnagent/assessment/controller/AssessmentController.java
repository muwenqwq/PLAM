package com.learnagent.assessment.controller;

import com.learnagent.assessment.dto.AssessmentSubmitRequest;
import com.learnagent.assessment.service.AssessmentService;
import com.learnagent.common.dto.ApiResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/assessment")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService assessmentService;

    @PostMapping("/submit")
    public ApiResult<Map<String, Object>> submit(@Valid @RequestBody AssessmentSubmitRequest req) {
        return ApiResult.ok(assessmentService.submitAssessment(req));
    }
}
