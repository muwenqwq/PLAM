package com.edustudio.module.auth.controller;

import com.edustudio.common.api.Result;
import com.edustudio.module.auth.dto.LoginRequest;
import com.edustudio.module.auth.dto.RegisterRequest;
import com.edustudio.module.auth.service.AuthService;
import com.edustudio.module.auth.vo.CurrentUserVO;
import com.edustudio.module.auth.vo.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证安全")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<CurrentUserVO> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @Operation(summary = "获取当前登录用户")
    @GetMapping("/me")
    public Result<CurrentUserVO> me() {
        return Result.success(authService.currentUser());
    }

    @Operation(summary = "用户退出")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success();
    }
}
