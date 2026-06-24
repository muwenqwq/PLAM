package com.edustudio.module.user.controller;

import com.edustudio.common.api.Result;
import com.edustudio.module.auth.service.AuthService;
import com.edustudio.module.auth.vo.CurrentUserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户资料")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

    @Operation(summary = "获取当前用户资料")
    @GetMapping("/me")
    public Result<CurrentUserVO> me() {
        return Result.success(authService.currentUser());
    }
}
