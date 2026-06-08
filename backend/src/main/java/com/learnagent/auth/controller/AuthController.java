package com.learnagent.auth.controller;

import com.learnagent.auth.dto.LoginRequest;
import com.learnagent.auth.dto.LoginResponse;
import com.learnagent.auth.service.AuthService;
import com.learnagent.common.dto.ApiResult;
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
