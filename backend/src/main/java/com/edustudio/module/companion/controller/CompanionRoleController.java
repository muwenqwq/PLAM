package com.edustudio.module.companion.controller;

import com.edustudio.common.api.PageResult;
import com.edustudio.common.api.Result;
import com.edustudio.module.companion.dto.CompanionRoleCreateRequest;
import com.edustudio.module.companion.dto.CompanionRoleQueryRequest;
import com.edustudio.module.companion.dto.CompanionRoleUpdateRequest;
import com.edustudio.module.companion.service.CompanionRoleService;
import com.edustudio.module.companion.vo.CompanionRoleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI 角色陪伴")
@Validated
@RestController
@RequestMapping("/api/companion-roles")
@RequiredArgsConstructor
public class CompanionRoleController {

    private final CompanionRoleService companionRoleService;

    @Operation(summary = "创建 AI 角色")
    @PostMapping
    public Result<CompanionRoleVO> create(@Valid @RequestBody CompanionRoleCreateRequest request) {
        return Result.success(companionRoleService.create(request));
    }

    @Operation(summary = "分页查询 AI 角色")
    @GetMapping
    public Result<PageResult<CompanionRoleVO>> page(@Valid CompanionRoleQueryRequest request) {
        return Result.success(companionRoleService.page(request));
    }

    @Operation(summary = "查询默认可用 AI 角色")
    @GetMapping("/active")
    public Result<CompanionRoleVO> active() {
        return Result.success(companionRoleService.active());
    }

    @Operation(summary = "查询 AI 角色详情")
    @GetMapping("/{id}")
    public Result<CompanionRoleVO> detail(@PathVariable Long id) {
        return Result.success(companionRoleService.detail(id));
    }

    @Operation(summary = "更新 AI 角色")
    @PutMapping("/{id}")
    public Result<CompanionRoleVO> update(
            @PathVariable Long id,
            @Valid @RequestBody CompanionRoleUpdateRequest request
    ) {
        return Result.success(companionRoleService.update(id, request));
    }

    @Operation(summary = "删除 AI 角色")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        companionRoleService.delete(id);
        return Result.success();
    }

    @Operation(summary = "设为默认 AI 角色")
    @PostMapping("/{id}/default")
    public Result<CompanionRoleVO> setDefault(@PathVariable Long id) {
        return Result.success(companionRoleService.setDefault(id));
    }
}
