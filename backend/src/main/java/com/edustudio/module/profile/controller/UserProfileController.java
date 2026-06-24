package com.edustudio.module.profile.controller;

import com.edustudio.common.api.Result;
import com.edustudio.module.profile.dto.UserProfileUpsertRequest;
import com.edustudio.module.profile.service.UserProfileService;
import com.edustudio.module.profile.vo.UserProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户画像")
@Validated
@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @Operation(summary = "查询当前用户全局画像")
    @GetMapping("/me")
    public Result<UserProfileVO> me() {
        return Result.success(userProfileService.getMe());
    }

    @Operation(summary = "创建或更新当前用户全局画像")
    @PutMapping("/me")
    public Result<UserProfileVO> upsertMe(@Valid @RequestBody UserProfileUpsertRequest request) {
        return Result.success(userProfileService.upsertMe(request));
    }

    @Operation(summary = "查询学习空间画像")
    @GetMapping("/space/{spaceId}")
    public Result<UserProfileVO> getBySpace(@PathVariable Long spaceId) {
        return Result.success(userProfileService.getBySpace(spaceId));
    }

    @Operation(summary = "创建或更新学习空间画像")
    @PutMapping("/space/{spaceId}")
    public Result<UserProfileVO> upsertBySpace(
            @PathVariable Long spaceId,
            @Valid @RequestBody UserProfileUpsertRequest request
    ) {
        return Result.success(userProfileService.upsertBySpace(spaceId, request));
    }
}
