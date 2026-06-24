package com.edustudio.module.profile.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.edustudio.common.security.LoginUserHolder;
import com.edustudio.common.utils.JsonUtils;
import com.edustudio.module.learningspace.service.LearningSpaceService;
import com.edustudio.module.profile.dto.UserProfileUpsertRequest;
import com.edustudio.module.profile.entity.UserProfile;
import com.edustudio.module.profile.mapper.UserProfileMapper;
import com.edustudio.module.profile.service.UserProfileService;
import com.edustudio.module.profile.vo.UserProfileVO;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl extends ServiceImpl<UserProfileMapper, UserProfile>
        implements UserProfileService {

    private static final long GLOBAL_PROFILE_SPACE_ID = 0L;
    private static final String ACTIVE_STATUS = "active";
    private static final String DEFAULT_PROFILE_SOURCE = "manual";

    private final LearningSpaceService learningSpaceService;

    @Override
    public UserProfileVO getMe() {
        Long userId = LoginUserHolder.requireCurrentUserId();
        UserProfile profile = findByUserAndSpace(userId, GLOBAL_PROFILE_SPACE_ID);
        return profile == null ? UserProfileVO.empty(userId, null) : toVO(profile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfileVO upsertMe(UserProfileUpsertRequest request) {
        return upsert(GLOBAL_PROFILE_SPACE_ID, request);
    }

    @Override
    public UserProfileVO getBySpace(Long spaceId) {
        learningSpaceService.assertOwned(spaceId);
        Long userId = LoginUserHolder.requireCurrentUserId();
        UserProfile profile = findByUserAndSpace(userId, spaceId);
        return profile == null ? UserProfileVO.empty(userId, spaceId) : toVO(profile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfileVO upsertBySpace(Long spaceId, UserProfileUpsertRequest request) {
        learningSpaceService.assertOwned(spaceId);
        return upsert(spaceId, request);
    }

    private UserProfileVO upsert(Long spaceId, UserProfileUpsertRequest request) {
        Long userId = LoginUserHolder.requireCurrentUserId();
        UserProfile profile = findByUserAndSpace(userId, spaceId);
        boolean create = profile == null;
        if (create) {
            profile = new UserProfile();
            profile.setUserId(userId);
            profile.setSpaceId(spaceId);
            profile.setWeeklyAvailableHours(BigDecimal.ZERO);
            profile.setProfileSource(DEFAULT_PROFILE_SOURCE);
            profile.setStatus(ACTIVE_STATUS);
        }
        applyRequest(profile, request);
        if (create) {
            save(profile);
        } else {
            updateById(profile);
        }
        return toVO(profile);
    }

    private UserProfile findByUserAndSpace(Long userId, Long spaceId) {
        return getOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId)
                .eq(UserProfile::getSpaceId, spaceId)
                .eq(UserProfile::getDeleted, 0)
                .last("LIMIT 1"), false);
    }

    private void applyRequest(UserProfile profile, UserProfileUpsertRequest request) {
        if (request.getRealName() != null) {
            profile.setRealName(request.getRealName());
        }
        if (request.getSchool() != null) {
            profile.setSchool(request.getSchool());
        }
        if (request.getMajor() != null) {
            profile.setMajor(request.getMajor());
        }
        if (request.getGradeLevel() != null) {
            profile.setGradeLevel(request.getGradeLevel());
        }
        if (request.getLearningGoal() != null) {
            profile.setLearningGoal(request.getLearningGoal());
        }
        if (request.getSubjectDirection() != null) {
            profile.setSubjectDirection(request.getSubjectDirection());
        }
        if (StringUtils.hasText(request.getFoundationLevel())) {
            profile.setFoundationLevel(request.getFoundationLevel());
        }
        if (request.getInterestTags() != null) {
            profile.setInterestTags(JsonUtils.toJson(request.getInterestTags()));
        }
        if (request.getWeakPoints() != null) {
            profile.setWeakPoints(JsonUtils.toJson(request.getWeakPoints()));
        }
        if (request.getTargetExam() != null) {
            profile.setTargetExam(request.getTargetExam());
        }
        if (request.getWeeklyAvailableHours() != null) {
            profile.setWeeklyAvailableHours(request.getWeeklyAvailableHours());
        }
        if (request.getAvailableTimeSlots() != null) {
            profile.setAvailableTimeSlots(JsonUtils.toJson(request.getAvailableTimeSlots()));
        }
        if (request.getOutputStyle() != null) {
            profile.setOutputStyle(request.getOutputStyle());
        }
        if (StringUtils.hasText(request.getProfileSource())) {
            profile.setProfileSource(request.getProfileSource());
        }
    }

    private UserProfileVO toVO(UserProfile profile) {
        return UserProfileVO.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .spaceId(apiSpaceId(profile.getSpaceId()))
                .realName(profile.getRealName())
                .school(profile.getSchool())
                .major(profile.getMajor())
                .gradeLevel(profile.getGradeLevel())
                .learningGoal(profile.getLearningGoal())
                .subjectDirection(profile.getSubjectDirection())
                .foundationLevel(profile.getFoundationLevel())
                .interestTags(parseJson(profile.getInterestTags()))
                .weakPoints(parseJson(profile.getWeakPoints()))
                .targetExam(profile.getTargetExam())
                .weeklyAvailableHours(profile.getWeeklyAvailableHours())
                .availableTimeSlots(parseJson(profile.getAvailableTimeSlots()))
                .outputStyle(profile.getOutputStyle())
                .profileSource(profile.getProfileSource())
                .status(profile.getStatus())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    private Long apiSpaceId(Long spaceId) {
        return spaceId == null || spaceId == GLOBAL_PROFILE_SPACE_ID ? null : spaceId;
    }

    private JsonNode parseJson(String json) {
        return StringUtils.hasText(json) ? JsonUtils.fromJson(json, JsonNode.class) : null;
    }
}
