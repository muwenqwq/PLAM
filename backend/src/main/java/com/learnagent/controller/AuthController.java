package com.learnagent.controller;

import com.learnagent.dto.request.LoginRequest;
import com.learnagent.dto.response.ApiResult;
import com.learnagent.dto.response.LoginResponse;
import com.learnagent.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 登录接口
     * POST /api/login
     */
    @PostMapping("/api/login")
    public ApiResult<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return ApiResult.ok(authService.login(req));
    }
}
