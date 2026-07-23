package com.edustudio.module.profile.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.edustudio.common.security.LoginUserHolder;
import com.edustudio.common.utils.JsonUtils;
import com.edustudio.integration.ai.AiServiceClient;
import com.edustudio.integration.ai.dto.AiProfileAnalyzeRequest;
import com.edustudio.integration.ai.dto.AiProfileAnalyzeResponse;
import com.edustudio.module.learningspace.service.LearningSpaceService;
import com.edustudio.module.modelprovider.service.ModelProviderService;
import com.edustudio.module.profile.dto.UserProfileUpsertRequest;
import com.edustudio.module.profile.entity.UserProfile;
import com.edustudio.module.profile.mapper.UserProfileMapper;
import com.edustudio.module.profile.service.UserProfileService;
import com.edustudio.module.profile.vo.UserProfileVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileServiceImpl extends ServiceImpl<UserProfileMapper, UserProfile>
        implements UserProfileService {

    private static final long GLOBAL_PROFILE_SPACE_ID = 0L;
    private static final String ACTIVE_STATUS = "active";
    private static final String DEFAULT_PROFILE_SOURCE = "manual";

    private final LearningSpaceService learningSpaceService;
    private final ModelProviderService modelProviderService;
    private final AiServiceClient aiServiceClient;

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
    @Transactional(rollbackFor = Exception.class)
    public UserProfileVO getBySpace(Long spaceId) {
        learningSpaceService.assertOwned(spaceId);
        Long userId = LoginUserHolder.requireCurrentUserId();
        UserProfile profile = findByUserAndSpace(userId, spaceId);
        profile = profile == null ? createDefaultSpaceProfile(userId, spaceId) : profile;
        sanitizeLegacySummary(profile);
        return toVO(profile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfileVO upsertBySpace(Long spaceId, UserProfileUpsertRequest request) {
        learningSpaceService.assertOwned(spaceId);
        return upsert(spaceId, request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfileVO reanalyzeBySpace(Long spaceId) {
        learningSpaceService.assertOwned(spaceId);
        Long userId = LoginUserHolder.requireCurrentUserId();
        UserProfile profile = findByUserAndSpace(userId, spaceId);
        if (profile == null) {
            profile = createDefaultSpaceProfile(userId, spaceId);
        }
        Map<String, Object> evidence = new LinkedHashMap<>();
        if (StringUtils.hasText(profile.getLastActivitySummary())) {
            evidence.put("previous_update_note", profile.getLastActivitySummary());
        }
        AiProfileAnalyzeResponse analysis = analyzeProfile(
                profile, "profile_refresh", profile.getSubjectDirection(), List.of(), parseStringList(profile.getWeakPoints()), evidence);
        applyAnalysis(profile, analysis);
        profile.setLastActivitySource("profile_refresh");
        profile.setLastActivitySummary(safeEvidenceSummary(analysis, "已重新分析现有学习记录，画像内容已按稳定学习特征整理。"));
        profile.setLastActivityAt(LocalDateTime.now());
        profile.setAdaptiveSummary(StringUtils.hasText(analysis == null ? null : analysis.getAdaptiveSummary())
                ? shorten(analysis.getAdaptiveSummary().trim(), 1500)
                : buildAdaptiveSummary(profile));
        updateById(profile);
        return toVO(profile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> getAiProfile(Long spaceId) {
        if (spaceId == null) {
            return Map.of();
        }
        Long userId = LoginUserHolder.requireCurrentUserId();
        UserProfile profile = findByUserAndSpace(userId, spaceId);
        if (profile == null) {
            profile = createDefaultSpaceProfile(userId, spaceId);
        }
        sanitizeLegacySummary(profile);
        return aiProfileValues(profile);
    }

    private Map<String, Object> aiProfileValues(UserProfile profile) {
        Map<String, Object> values = new LinkedHashMap<>();
        putIfText(values, "grade_level", profile.getGradeLevel());
        putIfText(values, "learning_goal", profile.getLearningGoal());
        putIfText(values, "profile_narrative", profile.getProfileNarrative());
        putIfText(values, "adaptive_summary", profile.getAdaptiveSummary());
        putIfText(values, "subject_direction", profile.getSubjectDirection());
        putIfText(values, "foundation_level", profile.getFoundationLevel());
        values.put("interest_tags", parseStringList(profile.getInterestTags()));
        values.put("weak_points", parseStringList(profile.getWeakPoints()));
        putIfText(values, "target_exam", profile.getTargetExam());
        values.put("weekly_available_hours", profile.getWeeklyAvailableHours());
        values.put("available_time_slots", parseStringList(profile.getAvailableTimeSlots()));
        putIfText(values, "output_style", profile.getOutputStyle());
        putIfText(values, "last_activity_source", profile.getLastActivitySource());
        if (profile.getLastActivityAt() != null) {
            values.put("last_activity_at", profile.getLastActivityAt().toString());
        }
        return values;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordActivity(Long spaceId, String subject, List<String> knowledgePoints,
                               List<String> weakPoints, String source, String activitySummary) {
        if (spaceId == null) {
            return;
        }
        Long userId = LoginUserHolder.requireCurrentUserId();
        UserProfile profile = findByUserAndSpace(userId, spaceId);
        boolean create = profile == null;
        if (create) {
            profile = new UserProfile();
            profile.setUserId(userId);
            profile.setSpaceId(spaceId);
            profile.setFoundationLevel("intermediate");
            profile.setWeeklyAvailableHours(BigDecimal.ZERO);
            profile.setOutputStyle("结构化 Markdown");
            profile.setStatus(ACTIVE_STATUS);
        }
        String activitySource = StringUtils.hasText(source) ? source : "adaptive";
        Map<String, Object> evidence = activityEvidence(activitySummary);
        evidence.putIfAbsent("source", activitySource);
        if (StringUtils.hasText(subject)) {
            evidence.putIfAbsent("subject", subject.trim());
        }
        List<String> safeKnowledgePoints = knowledgePoints == null ? List.of() : knowledgePoints;
        List<String> safeWeakPoints = weakPoints == null ? List.of() : weakPoints;
        if (!safeKnowledgePoints.isEmpty()) {
            evidence.putIfAbsent("knowledge_points", safeKnowledgePoints);
        }
        if (!safeWeakPoints.isEmpty()) {
            evidence.putIfAbsent("weak_points", safeWeakPoints);
        }
        AiProfileAnalyzeResponse analysis = analyzeProfile(
                profile, activitySource, subject, safeKnowledgePoints, safeWeakPoints, evidence);
        applyAnalysis(profile, analysis);
        if (analysis == null && "assessment".equals(activitySource) && !safeWeakPoints.isEmpty()) {
            profile.setWeakPoints(JsonUtils.toJson(mergeProfileValues(
                    parseStringList(profile.getWeakPoints()), null, safeWeakPoints)));
        }
        if (StringUtils.hasText(subject) && !StringUtils.hasText(profile.getSubjectDirection())) {
            profile.setSubjectDirection(subject.trim());
        }
        profile.setProfileSource(Boolean.TRUE.equals(analysis == null ? null : analysis.getShouldUpdate())
                ? "ai_adaptive" : profile.getProfileSource());
        if (!StringUtils.hasText(profile.getProfileSource())) {
            profile.setProfileSource("adaptive");
        }
        profile.setLastActivitySource(activitySource);
        profile.setLastActivitySummary(safeEvidenceSummary(
                analysis, activityDescription(activitySource, subject)));
        profile.setLastActivityAt(LocalDateTime.now());
        profile.setAdaptiveSummary(StringUtils.hasText(analysis == null ? null : analysis.getAdaptiveSummary())
                ? shorten(analysis.getAdaptiveSummary().trim(), 1500)
                : buildAdaptiveSummary(profile));
        profile.setStatus(ACTIVE_STATUS);
        if (create) {
            save(profile);
        } else {
            updateById(profile);
        }
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
        profile.setAdaptiveSummary(buildAdaptiveSummary(profile));
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

    private UserProfile createDefaultSpaceProfile(Long userId, Long spaceId) {
        var space = learningSpaceService.detail(spaceId);
        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setSpaceId(spaceId);
        profile.setSubjectDirection(space.getSubject());
        profile.setFoundationLevel("intermediate");
        profile.setInterestTags("[]");
        profile.setWeakPoints("[]");
        profile.setWeeklyAvailableHours(BigDecimal.ZERO);
        profile.setAvailableTimeSlots("[]");
        profile.setOutputStyle("结构化 Markdown");
        profile.setProfileSource("space_creation");
        profile.setAdaptiveSummary(buildAdaptiveSummary(profile));
        profile.setStatus(ACTIVE_STATUS);
        save(profile);
        return profile;
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
        if (request.getProfileNarrative() != null) {
            profile.setProfileNarrative(request.getProfileNarrative());
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
                .profileNarrative(profile.getProfileNarrative())
                .adaptiveSummary(profile.getAdaptiveSummary())
                .subjectDirection(profile.getSubjectDirection())
                .foundationLevel(profile.getFoundationLevel())
                .interestTags(parseJson(profile.getInterestTags()))
                .weakPoints(parseJson(profile.getWeakPoints()))
                .targetExam(profile.getTargetExam())
                .weeklyAvailableHours(profile.getWeeklyAvailableHours())
                .availableTimeSlots(parseJson(profile.getAvailableTimeSlots()))
                .outputStyle(profile.getOutputStyle())
                .profileSource(profile.getProfileSource())
                .lastActivitySource(profile.getLastActivitySource())
                .lastActivitySummary(profile.getLastActivitySummary())
                .lastActivityAt(profile.getLastActivityAt())
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

    private List<String> parseStringList(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return JsonUtils.fromJson(json, new TypeReference<List<String>>() {});
        } catch (RuntimeException ignored) {
            return Collections.emptyList();
        }
    }

    private List<String> mergeProfileValues(List<String> existing, String subject, List<String> additions) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        values.addAll(existing == null ? List.of() : existing);
        if (StringUtils.hasText(subject)) {
            values.add(subject.trim());
        }
        for (String value : additions == null ? List.<String>of() : additions) {
            if (StringUtils.hasText(value)) {
                values.add(value.trim());
            }
        }
        return new ArrayList<>(values).stream().limit(30).toList();
    }

    private AiProfileAnalyzeResponse analyzeProfile(
            UserProfile profile,
            String source,
            String subject,
            List<String> knowledgePoints,
            List<String> weakPoints,
            Map<String, Object> evidence
    ) {
        try {
            return aiServiceClient.analyzeProfile(AiProfileAnalyzeRequest.builder()
                    .modelConfig(modelProviderService.resolveConfig(null))
                    .currentProfile(aiProfileValues(profile))
                    .source(source)
                    .subject(subject)
                    .knowledgePoints(knowledgePoints == null ? List.of() : knowledgePoints)
                    .weakPoints(weakPoints == null ? List.of() : weakPoints)
                    .evidence(evidence == null ? Map.of() : evidence)
                    .build());
        } catch (RuntimeException exception) {
            log.warn("profile_analysis_failed source={} subject={} reason={}", source, subject, exception.getMessage());
            return null;
        }
    }

    private void applyAnalysis(UserProfile profile, AiProfileAnalyzeResponse analysis) {
        if (analysis == null || !Boolean.TRUE.equals(analysis.getShouldUpdate())) {
            return;
        }
        if (StringUtils.hasText(analysis.getProfileNarrative())) {
            profile.setProfileNarrative(shorten(analysis.getProfileNarrative().trim(), 4000));
        }
        if (StringUtils.hasText(analysis.getLearningGoal())) {
            profile.setLearningGoal(shorten(analysis.getLearningGoal().trim(), 1000));
        }
        if (StringUtils.hasText(analysis.getSubjectDirection())) {
            profile.setSubjectDirection(shorten(analysis.getSubjectDirection().trim(), 128));
        }
        String foundationLevel = analysis.getFoundationLevel();
        if (StringUtils.hasText(foundationLevel)
                && List.of("beginner", "intermediate", "advanced").contains(foundationLevel)) {
            profile.setFoundationLevel(foundationLevel);
        }
        if (analysis.getInterestTags() != null && !analysis.getInterestTags().isEmpty()) {
            profile.setInterestTags(JsonUtils.toJson(cleanValues(analysis.getInterestTags())));
        }
        if (analysis.getWeakPoints() != null && !analysis.getWeakPoints().isEmpty()) {
            profile.setWeakPoints(JsonUtils.toJson(cleanValues(analysis.getWeakPoints())));
        }
        if (analysis.getWeeklyAvailableHours() != null && analysis.getWeeklyAvailableHours().compareTo(BigDecimal.ZERO) >= 0) {
            profile.setWeeklyAvailableHours(analysis.getWeeklyAvailableHours().min(BigDecimal.valueOf(168)));
        }
        if (analysis.getAvailableTimeSlots() != null && !analysis.getAvailableTimeSlots().isEmpty()) {
            profile.setAvailableTimeSlots(JsonUtils.toJson(cleanValues(analysis.getAvailableTimeSlots())));
        }
        if (StringUtils.hasText(analysis.getOutputStyle())) {
            profile.setOutputStyle(shorten(analysis.getOutputStyle().trim(), 64));
        }
    }

    private List<String> cleanValues(List<String> values) {
        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .limit(30)
                .toList();
    }

    private Map<String, Object> activityEvidence(String activitySummary) {
        if (!StringUtils.hasText(activitySummary)) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, Object> parsed = JsonUtils.fromJson(activitySummary, new TypeReference<Map<String, Object>>() {});
            return new LinkedHashMap<>(parsed == null ? Map.of() : parsed);
        } catch (RuntimeException ignored) {
            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put("activity_summary", shorten(activitySummary.trim(), 2000));
            return evidence;
        }
    }

    private String safeEvidenceSummary(AiProfileAnalyzeResponse analysis, String fallback) {
        if (analysis != null && StringUtils.hasText(analysis.getEvidenceSummary())) {
            return shorten(analysis.getEvidenceSummary().trim(), 1000);
        }
        return fallback;
    }

    private void putIfText(Map<String, Object> values, String key, String value) {
        if (StringUtils.hasText(value)) {
            values.put(key, value);
        }
    }

    private String activityDescription(String source, String subject) {
        String subjectText = StringUtils.hasText(subject) ? "，主题为“" + subject.trim() + "”" : "";
        return switch (source == null ? "adaptive" : source) {
            case "chat" -> "AI 已分析本次学习对话；只有稳定的学习需求和表达偏好会写入画像。";
            case "assessment" -> "AI 已根据本次测验表现更新基础水平和待巩固知识点。";
            case "quiz_generation" -> "已在当前学习空间生成一份选择题测验，暂不据此判断掌握水平。";
            case "resource_generation", "agent_resource_generation" -> "已生成新的学习资源" + subjectText + "，画像仅保留可复用的学习方向。";
            case "learning_path" -> "已生成或调整学习路径" + subjectText + "。";
            case "knowledge_qa" -> "已完成一次资料问答，未从单次检索直接推断长期学习特征。";
            case "profile_refresh" -> "已重新分析现有学习记录并整理画像。";
            default -> "学习画像已根据最新学习行为重新评估。";
        };
    }

    private String buildAdaptiveSummary(UserProfile profile) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(profile.getProfileNarrative())) {
            parts.add("学生自述：" + shorten(profile.getProfileNarrative().trim(), 500));
        }
        if (StringUtils.hasText(profile.getLearningGoal())) {
            parts.add("当前目标是" + profile.getLearningGoal().trim());
        }
        if (StringUtils.hasText(profile.getSubjectDirection())) {
            parts.add("主要学习方向为" + profile.getSubjectDirection().trim());
        }
        if (StringUtils.hasText(profile.getFoundationLevel())) {
            parts.add("基础水平为" + foundationLabel(profile.getFoundationLevel()));
        }
        List<String> weakPoints = parseStringList(profile.getWeakPoints());
        if (!weakPoints.isEmpty()) {
            parts.add("当前需重点关注" + String.join("、", weakPoints.stream().limit(5).toList()));
        }
        List<String> interests = parseStringList(profile.getInterestTags());
        if (!interests.isEmpty()) {
            parts.add("更容易投入的内容包括" + String.join("、", interests.stream().limit(5).toList()));
        }
        return parts.isEmpty()
                ? "画像正在形成中。可以先用一段话描述自己的基础、目标、困难和期望的讲解方式。"
                : String.join("；", parts) + "。";
    }

    private void sanitizeLegacySummary(UserProfile profile) {
        String rebuilt = buildAdaptiveSummary(profile);
        boolean changed = !rebuilt.equals(profile.getAdaptiveSummary());
        if ("chat".equals(profile.getLastActivitySource())
                && StringUtils.hasText(profile.getLastActivitySummary())
                && (profile.getLastActivitySummary().contains("围绕“") || profile.getLastActivitySummary().contains("AI 对话："))) {
            profile.setLastActivitySummary("AI 已分析最近的学习对话；闲聊和原始提问不会直接写入学习画像。");
            changed = true;
        }
        if (changed) {
            profile.setAdaptiveSummary(rebuilt);
            updateById(profile);
        }
    }

    private String foundationLabel(String value) {
        return switch (value) {
            case "beginner" -> "入门";
            case "advanced" -> "进阶";
            default -> "中等";
        };
    }

    private String shorten(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "…";
    }
}
