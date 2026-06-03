package com.learnagent.controller;

import com.learnagent.dto.request.ProfileExtractRequest;
import com.learnagent.dto.response.ApiResult;
import com.learnagent.dto.response.ProfileResponse;
import com.learnagent.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @PostMapping("/extract")
    public ApiResult<ProfileResponse> extract(@Valid @RequestBody ProfileExtractRequest req) {
        return ApiResult.ok(profileService.extractProfile(req));
    }

    @GetMapping("/{studentId}")
    public ApiResult<ProfileResponse> getLatest(@PathVariable Long studentId) {
        return ApiResult.ok(profileService.getLatestProfile(studentId));
    }

    @GetMapping("/{studentId}/history")
    public ApiResult<List<ProfileResponse>> history(@PathVariable Long studentId) {
        return ApiResult.ok(profileService.getProfileHistory(studentId));
    }
}
