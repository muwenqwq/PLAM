package com.edustudio.module.profile.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.edustudio.common.security.LoginUserHolder;
import com.edustudio.common.utils.JsonUtils;
import com.edustudio.module.profile.dto.LearningPreferenceUpsertRequest;
import com.edustudio.module.profile.entity.LearningPreference;
import com.edustudio.module.profile.mapper.LearningPreferenceMapper;
import com.edustudio.module.profile.service.LearningPreferenceService;
import com.edustudio.module.profile.vo.LearningPreferenceVO;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class LearningPreferenceServiceImpl extends ServiceImpl<LearningPreferenceMapper, LearningPreference>
        implements LearningPreferenceService {

    private static final String ACTIVE_STATUS = "active";

    @Override
    public LearningPreferenceVO getMe() {
        Long userId = LoginUserHolder.requireCurrentUserId();
        LearningPreference preference = findByUserId(userId);
        return preference == null ? LearningPreferenceVO.defaults(userId) : toVO(preference);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LearningPreferenceVO upsertMe(LearningPreferenceUpsertRequest request) {
        Long userId = LoginUserHolder.requireCurrentUserId();
        LearningPreference preference = findByUserId(userId);
        boolean create = preference == null;
        if (create) {
            LearningPreferenceVO defaults = LearningPreferenceVO.defaults(userId);
            preference = new LearningPreference();
            preference.setUserId(userId);
            preference.setPreferredResourceTypes(JsonUtils.toJson(defaults.getPreferredResourceTypes()));
            preference.setOutputStyle(defaults.getOutputStyle());
            preference.setContentLengthPreference(defaults.getContentLengthPreference());
            preference.setDifficultyPreference(defaults.getDifficultyPreference());
            preference.setLanguagePreference(defaults.getLanguagePreference());
            preference.setStudyTimeSlots(JsonUtils.toJson(defaults.getStudyTimeSlots()));
            preference.setNotificationEnabled(defaults.getNotificationEnabled());
            preference.setKnowledgeGraphEnabled(defaults.getKnowledgeGraphEnabled());
            preference.setQuizEnabled(defaults.getQuizEnabled());
            preference.setReviewPlanEnabled(defaults.getReviewPlanEnabled());
            preference.setStatus(ACTIVE_STATUS);
        }
        applyRequest(preference, request);
        if (create) {
            save(preference);
        } else {
            updateById(preference);
        }
        return toVO(preference);
    }

    private LearningPreference findByUserId(Long userId) {
        return getOne(new LambdaQueryWrapper<LearningPreference>()
                .eq(LearningPreference::getUserId, userId)
                .eq(LearningPreference::getDeleted, 0)
                .last("LIMIT 1"), false);
    }

    private void applyRequest(LearningPreference preference, LearningPreferenceUpsertRequest request) {
        if (request.getPreferredResourceTypes() != null) {
            preference.setPreferredResourceTypes(JsonUtils.toJson(request.getPreferredResourceTypes()));
        }
        if (StringUtils.hasText(request.getOutputStyle())) {
            preference.setOutputStyle(request.getOutputStyle());
        }
        if (StringUtils.hasText(request.getContentLengthPreference())) {
            preference.setContentLengthPreference(request.getContentLengthPreference());
        }
        if (StringUtils.hasText(request.getDifficultyPreference())) {
            preference.setDifficultyPreference(request.getDifficultyPreference());
        }
        if (StringUtils.hasText(request.getLanguagePreference())) {
            preference.setLanguagePreference(request.getLanguagePreference());
        }
        if (request.getStudyTimeSlots() != null) {
            preference.setStudyTimeSlots(JsonUtils.toJson(request.getStudyTimeSlots()));
        }
        if (request.getNotificationEnabled() != null) {
            preference.setNotificationEnabled(request.getNotificationEnabled());
        }
        if (request.getKnowledgeGraphEnabled() != null) {
            preference.setKnowledgeGraphEnabled(request.getKnowledgeGraphEnabled());
        }
        if (request.getQuizEnabled() != null) {
            preference.setQuizEnabled(request.getQuizEnabled());
        }
        if (request.getReviewPlanEnabled() != null) {
            preference.setReviewPlanEnabled(request.getReviewPlanEnabled());
        }
    }

    private LearningPreferenceVO toVO(LearningPreference preference) {
        return LearningPreferenceVO.builder()
                .id(preference.getId())
                .userId(preference.getUserId())
                .preferredResourceTypes(parseJson(preference.getPreferredResourceTypes()))
                .outputStyle(preference.getOutputStyle())
                .contentLengthPreference(preference.getContentLengthPreference())
                .difficultyPreference(preference.getDifficultyPreference())
                .languagePreference(preference.getLanguagePreference())
                .studyTimeSlots(parseJson(preference.getStudyTimeSlots()))
                .notificationEnabled(preference.getNotificationEnabled())
                .knowledgeGraphEnabled(preference.getKnowledgeGraphEnabled())
                .quizEnabled(preference.getQuizEnabled())
                .reviewPlanEnabled(preference.getReviewPlanEnabled())
                .status(preference.getStatus())
                .createdAt(preference.getCreatedAt())
                .updatedAt(preference.getUpdatedAt())
                .build();
    }

    private JsonNode parseJson(String json) {
        return StringUtils.hasText(json) ? JsonUtils.fromJson(json, JsonNode.class) : null;
    }
}
