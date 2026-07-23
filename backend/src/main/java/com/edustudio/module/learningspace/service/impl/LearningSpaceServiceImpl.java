package com.edustudio.module.learningspace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.edustudio.common.api.PageResult;
import com.edustudio.common.api.ResultCode;
import com.edustudio.common.exception.BusinessException;
import com.edustudio.common.security.LoginUserHolder;
import com.edustudio.common.utils.JsonUtils;
import com.edustudio.module.agent.entity.AgentTask;
import com.edustudio.module.agent.mapper.AgentTaskMapper;
import com.edustudio.module.learningspace.dto.LearningSpaceCreateRequest;
import com.edustudio.module.learningspace.dto.LearningSpaceQueryRequest;
import com.edustudio.module.learningspace.dto.LearningSpaceUpdateRequest;
import com.edustudio.module.learningspace.entity.LearningSpace;
import com.edustudio.module.learningspace.mapper.LearningSpaceMapper;
import com.edustudio.module.learningspace.service.LearningSpaceService;
import com.edustudio.module.learningspace.support.LearningSpaceDataCleaner;
import com.edustudio.module.learningspace.vo.LearningSpaceSummaryVO;
import com.edustudio.module.learningspace.vo.LearningSpaceVO;
import com.edustudio.module.profile.entity.UserProfile;
import com.edustudio.module.profile.mapper.UserProfileMapper;
import com.edustudio.module.resource.entity.GeneratedResource;
import com.edustudio.module.resource.mapper.GeneratedResourceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LearningSpaceServiceImpl extends ServiceImpl<LearningSpaceMapper, LearningSpace>
        implements LearningSpaceService {

    private static final String ACTIVE_STATUS = "active";
    private static final String DEFAULT_VISIBILITY = "private";

    private final UserProfileMapper userProfileMapper;
    private final GeneratedResourceMapper generatedResourceMapper;
    private final AgentTaskMapper agentTaskMapper;
    private final LearningSpaceDataCleaner dataCleaner;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LearningSpaceVO create(LearningSpaceCreateRequest request) {
        Long userId = LoginUserHolder.requireCurrentUserId();
        boolean firstSpace = count(new LambdaQueryWrapper<LearningSpace>()
                .eq(LearningSpace::getUserId, userId)
                .eq(LearningSpace::getDeleted, 0)) == 0;

        LearningSpace entity = new LearningSpace();
        entity.setUserId(userId);
        entity.setSpaceName(request.getSpaceName());
        entity.setSubject(request.getSubject());
        entity.setDescription(request.getDescription());
        entity.setCoverUrl(request.getCoverUrl());
        entity.setVisibility(StringUtils.hasText(request.getVisibility())
                ? request.getVisibility()
                : DEFAULT_VISIBILITY);
        entity.setDefaultSpace(firstSpace);
        entity.setResourceCount(0);
        entity.setTaskCount(0);
        entity.setStatus(ACTIVE_STATUS);
        save(entity);
        createInitialProfile(userId, entity.getId(), request);
        return toVO(entity);
    }

    @Override
    public PageResult<LearningSpaceVO> page(LearningSpaceQueryRequest request) {
        Long userId = LoginUserHolder.requireCurrentUserId();
        LambdaQueryWrapper<LearningSpace> wrapper = new LambdaQueryWrapper<LearningSpace>()
                .eq(LearningSpace::getUserId, userId)
                .eq(LearningSpace::getDeleted, 0)
                .eq(StringUtils.hasText(request.getStatus()), LearningSpace::getStatus, request.getStatus())
                .eq(StringUtils.hasText(request.getSubject()), LearningSpace::getSubject, request.getSubject())
                .and(StringUtils.hasText(request.getKeyword()), query -> query
                        .like(LearningSpace::getSpaceName, request.getKeyword())
                        .or()
                        .like(LearningSpace::getSubject, request.getKeyword()))
                .orderByDesc(LearningSpace::getDefaultSpace)
                .orderByDesc(LearningSpace::getUpdatedAt);
        Page<LearningSpace> result = page(new Page<>(request.getPageNum(), request.getPageSize()), wrapper);
        List<LearningSpaceVO> records = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public LearningSpaceVO detail(Long id) {
        return toVO(getOwnedEntity(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LearningSpaceVO update(Long id, LearningSpaceUpdateRequest request) {
        LearningSpace entity = getOwnedEntity(id);
        if (StringUtils.hasText(request.getSpaceName())) {
            entity.setSpaceName(request.getSpaceName());
        }
        if (StringUtils.hasText(request.getSubject())) {
            entity.setSubject(request.getSubject());
            UserProfile profile = userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                    .eq(UserProfile::getUserId, entity.getUserId())
                    .eq(UserProfile::getSpaceId, entity.getId())
                    .eq(UserProfile::getDeleted, 0)
                    .last("LIMIT 1"));
            if (profile != null) {
                profile.setSubjectDirection(request.getSubject());
                userProfileMapper.updateById(profile);
            }
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getCoverUrl() != null) {
            entity.setCoverUrl(request.getCoverUrl());
        }
        if (StringUtils.hasText(request.getVisibility())) {
            entity.setVisibility(request.getVisibility());
        }
        if (StringUtils.hasText(request.getStatus())) {
            entity.setStatus(request.getStatus());
        }
        updateById(entity);
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        LearningSpace entity = getOwnedEntity(id);
        Long userId = entity.getUserId();
        boolean wasDefault = Boolean.TRUE.equals(entity.getDefaultSpace());
        dataCleaner.deleteAll(userId, entity.getId());
        removeById(entity.getId());
        if (wasDefault) {
            promoteNewestSpaceAsDefault(userId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LearningSpaceVO setDefault(Long id) {
        LearningSpace entity = getOwnedEntity(id);
        lambdaUpdate()
                .eq(LearningSpace::getUserId, entity.getUserId())
                .eq(LearningSpace::getDeleted, 0)
                .set(LearningSpace::getDefaultSpace, false)
                .update();
        entity.setDefaultSpace(true);
        updateById(entity);
        return toVO(entity);
    }

    @Override
    public LearningSpaceVO getDefault() {
        Long userId = LoginUserHolder.requireCurrentUserId();
        LearningSpace entity = getOne(new LambdaQueryWrapper<LearningSpace>()
                .eq(LearningSpace::getUserId, userId)
                .eq(LearningSpace::getDefaultSpace, true)
                .eq(LearningSpace::getDeleted, 0)
                .last("LIMIT 1"), false);
        return entity == null ? null : toVO(entity);
    }

    @Override
    public LearningSpaceSummaryVO summary(Long id) {
        LearningSpace entity = getOwnedEntity(id);
        Long userId = entity.getUserId();
        ensureSpaceProfile(entity);
        int resourceCount = Math.toIntExact(generatedResourceMapper.selectCount(new LambdaQueryWrapper<GeneratedResource>()
                .eq(GeneratedResource::getUserId, userId)
                .eq(GeneratedResource::getSpaceId, id)
                .eq(GeneratedResource::getDeleted, 0)));
        int taskCount = Math.toIntExact(agentTaskMapper.selectCount(new LambdaQueryWrapper<AgentTask>()
                .eq(AgentTask::getUserId, userId)
                .eq(AgentTask::getSpaceId, id)
                .eq(AgentTask::getDeleted, 0)));
        int profileCount = Math.toIntExact(userProfileMapper.selectCount(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId)
                .eq(UserProfile::getSpaceId, id)
                .eq(UserProfile::getDeleted, 0)));
        return LearningSpaceSummaryVO.builder()
                .id(entity.getId())
                .spaceName(entity.getSpaceName())
                .subject(entity.getSubject())
                .resourceCount(resourceCount)
                .taskCount(taskCount)
                .profileCount(profileCount)
                .generatedResourceCount(resourceCount)
                .activeTaskCount(taskCount)
                .upcomingTaskCount(0)
                .build();
    }

    @Override
    public void assertOwned(Long id) {
        getOwnedEntity(id);
    }

    private LearningSpace getOwnedEntity(Long id) {
        Long userId = LoginUserHolder.requireCurrentUserId();
        LearningSpace entity = getOne(new LambdaQueryWrapper<LearningSpace>()
                .eq(LearningSpace::getId, id)
                .eq(LearningSpace::getUserId, userId)
                .eq(LearningSpace::getDeleted, 0), false);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "学习空间不存在或无权访问");
        }
        return entity;
    }

    @Override
    public void incrementResourceCount(Long spaceId) {
        lambdaUpdate()
                .eq(LearningSpace::getId, spaceId)
                .setSql("resource_count = resource_count + 1")
                .update();
    }

    @Override
    public void incrementTaskCount(Long spaceId) {
        lambdaUpdate()
                .eq(LearningSpace::getId, spaceId)
                .setSql("task_count = task_count + 1")
                .update();
    }

    private void promoteNewestSpaceAsDefault(Long userId) {
        LearningSpace next = getOne(new LambdaQueryWrapper<LearningSpace>()
                .eq(LearningSpace::getUserId, userId)
                .eq(LearningSpace::getDeleted, 0)
                .orderByDesc(LearningSpace::getUpdatedAt)
                .last("LIMIT 1"), false);
        if (next == null) {
            return;
        }
        next.setDefaultSpace(true);
        updateById(next);
    }

    private void createInitialProfile(Long userId, Long spaceId, LearningSpaceCreateRequest request) {
        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setSpaceId(spaceId);
        profile.setLearningGoal(request.getLearningGoal());
        profile.setSubjectDirection(request.getSubject());
        profile.setFoundationLevel(StringUtils.hasText(request.getFoundationLevel())
                ? request.getFoundationLevel()
                : "intermediate");
        profile.setInterestTags("[]");
        profile.setWeakPoints(JsonUtils.toJson(request.getWeakPoints() == null ? List.of() : request.getWeakPoints()));
        profile.setWeeklyAvailableHours(request.getWeeklyAvailableHours() == null
                ? BigDecimal.ZERO
                : request.getWeeklyAvailableHours());
        profile.setAvailableTimeSlots(JsonUtils.toJson(request.getAvailableTimeSlots() == null
                ? List.of()
                : request.getAvailableTimeSlots()));
        profile.setOutputStyle(StringUtils.hasText(request.getOutputStyle())
                ? request.getOutputStyle()
                : "结构化 Markdown");
        profile.setProfileSource("space_creation");
        profile.setStatus(ACTIVE_STATUS);
        userProfileMapper.insert(profile);
    }

    private void ensureSpaceProfile(LearningSpace space) {
        Long count = userProfileMapper.selectCount(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, space.getUserId())
                .eq(UserProfile::getSpaceId, space.getId())
                .eq(UserProfile::getDeleted, 0));
        if (count > 0) {
            return;
        }
        UserProfile profile = new UserProfile();
        profile.setUserId(space.getUserId());
        profile.setSpaceId(space.getId());
        profile.setSubjectDirection(space.getSubject());
        profile.setFoundationLevel("intermediate");
        profile.setInterestTags("[]");
        profile.setWeakPoints("[]");
        profile.setWeeklyAvailableHours(BigDecimal.ZERO);
        profile.setAvailableTimeSlots("[]");
        profile.setOutputStyle("结构化 Markdown");
        profile.setProfileSource("space_creation");
        profile.setStatus(ACTIVE_STATUS);
        userProfileMapper.insert(profile);
    }

    private LearningSpaceVO toVO(LearningSpace entity) {
        return LearningSpaceVO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .spaceName(entity.getSpaceName())
                .subject(entity.getSubject())
                .description(entity.getDescription())
                .coverUrl(entity.getCoverUrl())
                .visibility(entity.getVisibility())
                .defaultSpace(Boolean.TRUE.equals(entity.getDefaultSpace()))
                .resourceCount(entity.getResourceCount())
                .taskCount(entity.getTaskCount())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
