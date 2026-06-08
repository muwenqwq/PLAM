package com.learnagent.tutor.controller;

import com.learnagent.tutor.dto.TutorAskRequest;
import com.learnagent.tutor.dto.TutorResponse;
import com.learnagent.tutor.service.TutorService;
import com.learnagent.common.dto.ApiResult;
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
