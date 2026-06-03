package com.learnagent.controller;

import com.learnagent.dto.request.AssessmentSubmitRequest;
import com.learnagent.dto.response.ApiResult;
import com.learnagent.service.AssessmentService;
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
