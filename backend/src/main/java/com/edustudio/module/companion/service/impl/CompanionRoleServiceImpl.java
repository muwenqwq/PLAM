package com.edustudio.module.companion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.edustudio.common.api.PageResult;
import com.edustudio.common.api.ResultCode;
import com.edustudio.common.exception.BusinessException;
import com.edustudio.common.security.LoginUserHolder;
import com.edustudio.module.companion.dto.CompanionRoleCreateRequest;
import com.edustudio.module.companion.dto.CompanionRoleQueryRequest;
import com.edustudio.module.companion.dto.CompanionRoleUpdateRequest;
import com.edustudio.module.companion.entity.CompanionRole;
import com.edustudio.module.companion.mapper.CompanionRoleMapper;
import com.edustudio.module.companion.service.CompanionRoleService;
import com.edustudio.module.companion.vo.CompanionRoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanionRoleServiceImpl extends ServiceImpl<CompanionRoleMapper, CompanionRole>
        implements CompanionRoleService {

    private static final String ACTIVE_STATUS = "active";
    private static final String DISABLED_STATUS = "disabled";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompanionRoleVO create(CompanionRoleCreateRequest request) {
        Long userId = LoginUserHolder.requireCurrentUserId();
        boolean firstRole = count(new LambdaQueryWrapper<CompanionRole>()
                .eq(CompanionRole::getUserId, userId)
                .eq(CompanionRole::getDeleted, 0)) == 0;
        boolean defaultRole = Boolean.TRUE.equals(request.getDefaultRole()) || firstRole;
        if (defaultRole) {
            clearDefault(userId);
        }

        CompanionRole entity = new CompanionRole();
        entity.setUserId(userId);
        entity.setRoleName(request.getRoleName());
        entity.setRoleIdentity(request.getRoleIdentity());
        entity.setAvatarUrl(request.getAvatarUrl());
        entity.setThemeColor(defaultText(request.getThemeColor(), "#409EFF"));
        entity.setBackground(request.getBackground());
        entity.setPersonality(request.getPersonality());
        entity.setExpertise(request.getExpertise());
        entity.setHobbies(request.getHobbies());
        entity.setSpeakingStyle(request.getSpeakingStyle());
        entity.setScenario(request.getScenario());
        entity.setCompanionGoal(request.getCompanionGoal());
        entity.setBoundaries(request.getBoundaries());
        entity.setCustomPrompt(request.getCustomPrompt());
        entity.setTags(request.getTags());
        entity.setDefaultRole(defaultRole);
        entity.setStatus(ACTIVE_STATUS);
        save(entity);
        return toVO(entity);
    }

    @Override
    public PageResult<CompanionRoleVO> page(CompanionRoleQueryRequest request) {
        Long userId = LoginUserHolder.requireCurrentUserId();
        LambdaQueryWrapper<CompanionRole> wrapper = new LambdaQueryWrapper<CompanionRole>()
                .eq(CompanionRole::getUserId, userId)
                .eq(CompanionRole::getDeleted, 0)
                .eq(request.getDefaultRole() != null, CompanionRole::getDefaultRole, request.getDefaultRole())
                .eq(StringUtils.hasText(request.getStatus()), CompanionRole::getStatus, request.getStatus())
                .and(StringUtils.hasText(request.getKeyword()), query -> query
                        .like(CompanionRole::getRoleName, request.getKeyword())
                        .or()
                        .like(CompanionRole::getRoleIdentity, request.getKeyword())
                        .or()
                        .like(CompanionRole::getTags, request.getKeyword()))
                .orderByDesc(CompanionRole::getDefaultRole)
                .orderByDesc(CompanionRole::getUpdatedAt);
        Page<CompanionRole> result = page(new Page<>(request.getPageNum(), request.getPageSize()), wrapper);
        List<CompanionRoleVO> records = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public CompanionRoleVO detail(Long id) {
        return toVO(getOwnedEntity(id));
    }

    @Override
    public CompanionRoleVO active() {
        Long userId = LoginUserHolder.requireCurrentUserId();
        CompanionRole role = getOne(new LambdaQueryWrapper<CompanionRole>()
                .eq(CompanionRole::getUserId, userId)
                .eq(CompanionRole::getDefaultRole, true)
                .eq(CompanionRole::getStatus, ACTIVE_STATUS)
                .eq(CompanionRole::getDeleted, 0)
                .last("LIMIT 1"), false);
        if (role == null) {
            role = getOne(new LambdaQueryWrapper<CompanionRole>()
                    .eq(CompanionRole::getUserId, userId)
                    .eq(CompanionRole::getStatus, ACTIVE_STATUS)
                    .eq(CompanionRole::getDeleted, 0)
                    .orderByDesc(CompanionRole::getUpdatedAt)
                    .last("LIMIT 1"), false);
        }
        return role == null ? null : toVO(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompanionRoleVO update(Long id, CompanionRoleUpdateRequest request) {
        CompanionRole entity = getOwnedEntity(id);
        if (StringUtils.hasText(request.getRoleName())) {
            entity.setRoleName(request.getRoleName());
        }
        if (request.getRoleIdentity() != null) {
            entity.setRoleIdentity(request.getRoleIdentity());
        }
        if (request.getAvatarUrl() != null) {
            entity.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getThemeColor() != null) {
            entity.setThemeColor(request.getThemeColor());
        }
        if (request.getBackground() != null) {
            entity.setBackground(request.getBackground());
        }
        if (request.getPersonality() != null) {
            entity.setPersonality(request.getPersonality());
        }
        if (request.getExpertise() != null) {
            entity.setExpertise(request.getExpertise());
        }
        if (request.getHobbies() != null) {
            entity.setHobbies(request.getHobbies());
        }
        if (request.getSpeakingStyle() != null) {
            entity.setSpeakingStyle(request.getSpeakingStyle());
        }
        if (request.getScenario() != null) {
            entity.setScenario(request.getScenario());
        }
        if (request.getCompanionGoal() != null) {
            entity.setCompanionGoal(request.getCompanionGoal());
        }
        if (request.getBoundaries() != null) {
            entity.setBoundaries(request.getBoundaries());
        }
        if (request.getCustomPrompt() != null) {
            entity.setCustomPrompt(request.getCustomPrompt());
        }
        if (request.getTags() != null) {
            entity.setTags(request.getTags());
        }
        if (StringUtils.hasText(request.getStatus())) {
            entity.setStatus(request.getStatus());
        }
        if (Boolean.TRUE.equals(request.getDefaultRole())) {
            clearDefault(entity.getUserId());
            entity.setDefaultRole(true);
            entity.setStatus(ACTIVE_STATUS);
        } else if (Boolean.FALSE.equals(request.getDefaultRole())) {
            entity.setDefaultRole(false);
        }
        updateById(entity);
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        CompanionRole entity = getOwnedEntity(id);
        Long userId = entity.getUserId();
        boolean wasDefault = Boolean.TRUE.equals(entity.getDefaultRole());
        removeById(entity.getId());
        if (wasDefault) {
            promoteNewestAsDefault(userId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompanionRoleVO setDefault(Long id) {
        CompanionRole entity = getOwnedEntity(id);
        if (DISABLED_STATUS.equals(entity.getStatus())) {
            entity.setStatus(ACTIVE_STATUS);
        }
        clearDefault(entity.getUserId());
        entity.setDefaultRole(true);
        updateById(entity);
        return toVO(entity);
    }

    @Override
    public CompanionRole getOwnedActiveRole(Long id) {
        Long userId = LoginUserHolder.requireCurrentUserId();
        CompanionRole entity;
        if (id == null) {
            entity = getOne(new LambdaQueryWrapper<CompanionRole>()
                    .eq(CompanionRole::getUserId, userId)
                    .eq(CompanionRole::getDefaultRole, true)
                    .eq(CompanionRole::getStatus, ACTIVE_STATUS)
                    .eq(CompanionRole::getDeleted, 0)
                    .last("LIMIT 1"), false);
            if (entity == null) {
                entity = getOne(new LambdaQueryWrapper<CompanionRole>()
                        .eq(CompanionRole::getUserId, userId)
                        .eq(CompanionRole::getStatus, ACTIVE_STATUS)
                        .eq(CompanionRole::getDeleted, 0)
                        .orderByDesc(CompanionRole::getUpdatedAt)
                        .last("LIMIT 1"), false);
            }
            return entity;
        }
        entity = getOne(new LambdaQueryWrapper<CompanionRole>()
                .eq(CompanionRole::getId, id)
                .eq(CompanionRole::getUserId, userId)
                .eq(CompanionRole::getStatus, ACTIVE_STATUS)
                .eq(CompanionRole::getDeleted, 0), false);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "AI 角色不存在、未启用或无权访问");
        }
        return entity;
    }

    private CompanionRole getOwnedEntity(Long id) {
        CompanionRole entity = getOne(new LambdaQueryWrapper<CompanionRole>()
                .eq(CompanionRole::getId, id)
                .eq(CompanionRole::getUserId, LoginUserHolder.requireCurrentUserId())
                .eq(CompanionRole::getDeleted, 0), false);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "AI 角色不存在或无权访问");
        }
        return entity;
    }

    private void clearDefault(Long userId) {
        lambdaUpdate()
                .eq(CompanionRole::getUserId, userId)
                .eq(CompanionRole::getDeleted, 0)
                .set(CompanionRole::getDefaultRole, false)
                .update();
    }

    private void promoteNewestAsDefault(Long userId) {
        CompanionRole next = getOne(new LambdaQueryWrapper<CompanionRole>()
                .eq(CompanionRole::getUserId, userId)
                .eq(CompanionRole::getStatus, ACTIVE_STATUS)
                .eq(CompanionRole::getDeleted, 0)
                .orderByDesc(CompanionRole::getUpdatedAt)
                .last("LIMIT 1"), false);
        if (next != null) {
            next.setDefaultRole(true);
            updateById(next);
        }
    }

    private CompanionRoleVO toVO(CompanionRole entity) {
        return CompanionRoleVO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .roleName(entity.getRoleName())
                .roleIdentity(entity.getRoleIdentity())
                .avatarUrl(entity.getAvatarUrl())
                .themeColor(entity.getThemeColor())
                .background(entity.getBackground())
                .personality(entity.getPersonality())
                .expertise(entity.getExpertise())
                .hobbies(entity.getHobbies())
                .speakingStyle(entity.getSpeakingStyle())
                .scenario(entity.getScenario())
                .companionGoal(entity.getCompanionGoal())
                .boundaries(entity.getBoundaries())
                .customPrompt(entity.getCustomPrompt())
                .tags(entity.getTags())
                .defaultRole(Boolean.TRUE.equals(entity.getDefaultRole()))
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}
