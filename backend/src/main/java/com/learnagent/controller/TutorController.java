package com.learnagent.controller;

import com.learnagent.dto.request.TutorAskRequest;
import com.learnagent.dto.response.ApiResult;
import com.learnagent.dto.response.TutorResponse;
import com.learnagent.service.TutorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tutor")
@RequiredArgsConstructor
public class TutorController {

    private final TutorService tutorService;

    @PostMapping("/ask")
    public ApiResult<TutorResponse> ask(@Valid @RequestBody TutorAskRequest req) {
        return ApiResult.ok(tutorService.askTutor(req));
    }
}
