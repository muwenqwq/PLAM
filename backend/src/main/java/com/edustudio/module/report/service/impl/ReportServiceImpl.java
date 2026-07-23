package com.edustudio.module.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edustudio.common.api.ResultCode;
import com.edustudio.common.exception.BusinessException;
import com.edustudio.common.security.LoginUserHolder;
import com.edustudio.common.utils.JsonUtils;
import com.edustudio.integration.ai.AiServiceClient;
import com.edustudio.integration.ai.dto.AiReportGenerateRequest;
import com.edustudio.integration.ai.dto.AiReportGenerateResponse;
import com.edustudio.module.companion.entity.CompanionRole;
import com.edustudio.module.companion.service.CompanionRoleService;
import com.edustudio.module.companion.support.CompanionRolePayload;
import com.edustudio.module.chat.entity.Conversation;
import com.edustudio.module.chat.mapper.ConversationMapper;
import com.edustudio.module.learningpath.entity.LearningPath;
import com.edustudio.module.learningpath.entity.LearningPathItem;
import com.edustudio.module.learningpath.mapper.LearningPathItemMapper;
import com.edustudio.module.learningpath.mapper.LearningPathMapper;
import com.edustudio.module.learningspace.entity.LearningSpace;
import com.edustudio.module.learningspace.mapper.LearningSpaceMapper;
import com.edustudio.module.learningspace.service.LearningSpaceService;
import com.edustudio.module.modelprovider.service.ModelProviderService;
import com.edustudio.module.knowledge.entity.KnowledgeFile;
import com.edustudio.module.knowledge.mapper.KnowledgeFileMapper;
import com.edustudio.module.profile.entity.MasteryRecord;
import com.edustudio.module.profile.mapper.MasteryRecordMapper;
import com.edustudio.module.profile.service.UserProfileService;
import com.edustudio.module.quiz.entity.Quiz;
import com.edustudio.module.quiz.entity.QuizAnswer;
import com.edustudio.module.quiz.mapper.QuizAnswerMapper;
import com.edustudio.module.quiz.mapper.QuizMapper;
import com.edustudio.module.report.dto.ReportGenerateRequest;
import com.edustudio.module.report.entity.LearningReport;
import com.edustudio.module.report.mapper.LearningReportMapper;
import com.edustudio.module.report.service.ReportService;
import com.edustudio.module.report.vo.LearningReportVO;
import com.edustudio.module.report.vo.ReportOverviewVO;
import com.edustudio.module.resource.entity.GeneratedResource;
import com.edustudio.module.resource.mapper.GeneratedResourceMapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final LearningReportMapper learningReportMapper;
    private final LearningSpaceMapper learningSpaceMapper;
    private final GeneratedResourceMapper generatedResourceMapper;
    private final QuizMapper quizMapper;
    private final QuizAnswerMapper quizAnswerMapper;
    private final LearningPathMapper learningPathMapper;
    private final LearningPathItemMapper learningPathItemMapper;
    private final ConversationMapper conversationMapper;
    private final KnowledgeFileMapper knowledgeFileMapper;
    private final MasteryRecordMapper masteryRecordMapper;
    private final LearningSpaceService learningSpaceService;
    private final ModelProviderService modelProviderService;
    private final AiServiceClient aiServiceClient;
    private final CompanionRoleService companionRoleService;
    private final UserProfileService userProfileService;

    @Override
    public ReportOverviewVO overview(Long spaceId) {
        Long userId = LoginUserHolder.requireCurrentUserId();
        if (spaceId != null) {
            learningSpaceService.assertOwned(spaceId);
        }
        List<Quiz> quizzes = quizMapper.selectList(baseQuiz(userId, spaceId));
        List<MasteryRecord> mastery = masteryRecordMapper.selectList(new LambdaQueryWrapper<MasteryRecord>()
                .eq(MasteryRecord::getUserId, userId)
                .eq(spaceId != null, MasteryRecord::getSpaceId, spaceId)
                .eq(MasteryRecord::getDeleted, 0));
        return ReportOverviewVO.builder()
                .spaceCount(spaceId == null
                        ? learningSpaceMapper.selectCount(new LambdaQueryWrapper<LearningSpace>().eq(LearningSpace::getUserId, userId).eq(LearningSpace::getDeleted, 0))
                        : 1L)
                .resourceCount(generatedResourceMapper.selectCount(new LambdaQueryWrapper<GeneratedResource>()
                        .eq(GeneratedResource::getUserId, userId)
                        .eq(spaceId != null, GeneratedResource::getSpaceId, spaceId)
                        .eq(GeneratedResource::getDeleted, 0)))
                .quizCount((long) quizzes.size())
                .pathCount(learningPathMapper.selectCount(new LambdaQueryWrapper<LearningPath>()
                        .eq(LearningPath::getUserId, userId)
                        .eq(spaceId != null, LearningPath::getSpaceId, spaceId)
                        .eq(LearningPath::getDeleted, 0)))
                .averageScore(averageQuizScore(userId, quizzes))
                .averageMastery(avg(mastery.stream().map(MasteryRecord::getMasteryLevel).toList()))
                .build();
    }

    @Override
    public List<LearningReportVO> listBySpace(Long spaceId) {
        Long userId = LoginUserHolder.requireCurrentUserId();
        learningSpaceService.assertOwned(spaceId);
        return learningReportMapper.selectList(new LambdaQueryWrapper<LearningReport>()
                        .eq(LearningReport::getUserId, userId)
                        .eq(LearningReport::getSpaceId, spaceId)
                        .eq(LearningReport::getDeleted, 0)
                        .orderByDesc(LearningReport::getCreatedAt))
                .stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LearningReportVO generate(ReportGenerateRequest request) {
        Long userId = LoginUserHolder.requireCurrentUserId();
        learningSpaceService.assertOwned(request.getSpaceId());
        Long providerId = modelProviderService.resolveProviderId(request.getModelProviderId());
        CompanionRole role = resolveRole(request.getRolePlayEnabled(), request.getCompanionRoleId());
        Map<String, Object> rolePayload = CompanionRolePayload.from(role);
        ReportOverviewVO overview = overview(request.getSpaceId());
        List<MasteryRecord> masteryEntities = masteryRecordMapper.selectList(new LambdaQueryWrapper<MasteryRecord>()
                        .eq(MasteryRecord::getUserId, userId)
                        .eq(MasteryRecord::getSpaceId, request.getSpaceId())
                        .eq(MasteryRecord::getDeleted, 0)
                .orderByAsc(MasteryRecord::getMasteryLevel));
        List<Map<String, Object>> masteryRecords = masteryEntities.stream()
                .map(record -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("knowledge_point", record.getKnowledgePoint());
                    item.put("subject", record.getSubject());
                    item.put("mastery_level", record.getMasteryLevel());
                    item.put("weakness_level", record.getWeaknessLevel());
                    item.put("review_count", record.getReviewCount());
                    item.put("status", record.getStatus());
                    return item;
                })
                .toList();
        List<String> weakPoints = masteryEntities.stream()
                .filter(record -> record.getMasteryLevel() == null
                        || record.getMasteryLevel().compareTo(BigDecimal.valueOf(60)) < 0)
                .map(MasteryRecord::getKnowledgePoint)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        List<String> strengths = masteryEntities.stream()
                .filter(record -> record.getMasteryLevel() != null
                        && record.getMasteryLevel().compareTo(BigDecimal.valueOf(80)) >= 0)
                .map(MasteryRecord::getKnowledgePoint)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        List<String> nextActions = buildNextActions(overview, weakPoints);
        Map<String, Object> learnerProfile = userProfileService.getAiProfile(request.getSpaceId());
        Map<String, Object> overviewPayload = overviewPayload(overview);
        Map<String, Object> learningEvidence = buildLearningEvidence(userId, request.getSpaceId(), overview);
        AiReportGenerateResponse response = aiServiceClient.generateReport(AiReportGenerateRequest.builder()
                .modelConfig(modelProviderService.resolveConfig(providerId))
                .reportType(request.getReportType())
                .title(request.getTitle())
                .overview(overviewPayload)
                .masteryRecords(masteryRecords)
                .learningEvidence(learningEvidence)
                .profile(learnerProfile)
                .rolePlayEnabled(role != null)
                .companionRole(rolePayload)
                .build());
        Map<String, Object> reportJson = new LinkedHashMap<>();
        if (response != null && response.getReportJson() != null) {
            reportJson.putAll(response.getReportJson());
        }
        reportJson.put("overview", overviewPayload);
        reportJson.put("weak_points", weakPoints);
        reportJson.put("strengths", strengths);
        reportJson.put("next_actions", nextActions);
        reportJson.put("mastery_records", masteryRecords);
        reportJson.put("learner_profile", learnerProfile);
        reportJson.put("activity_summary", learningEvidence.get("activity_summary"));
        reportJson.put("resource_breakdown", learningEvidence.get("resource_breakdown"));
        reportJson.put("path_progress", learningEvidence.get("path_progress"));
        reportJson.put("quiz_performance", learningEvidence.get("quiz_performance"));
        reportJson.put("knowledge_library", learningEvidence.get("knowledge_library"));
        reportJson.put("recent_outputs", learningEvidence.get("recent_outputs"));
        reportJson.putIfAbsent("learning_observations", buildLearningObservations(learningEvidence));
        Map<String, Object> chartData = new LinkedHashMap<>();
        chartData.put("overview", List.of(
                Map.of("name", "资源", "value", overview.getResourceCount()),
                Map.of("name", "测验", "value", overview.getQuizCount()),
                Map.of("name", "路径", "value", overview.getPathCount())));
        chartData.put("mastery", masteryRecords);
        LearningReport report = new LearningReport();
        report.setUserId(userId);
        report.setSpaceId(request.getSpaceId());
        report.setReportType(request.getReportType());
        report.setTitle(response != null && StringUtils.hasText(response.getTitle()) ? response.getTitle() : request.getTitle());
        report.setSummary(response != null && StringUtils.hasText(response.getSummary())
                ? response.getSummary()
                : buildSummary(overview, weakPoints));
        report.setReportJson(JsonUtils.toJson(reportJson));
        report.setChartDataJson(JsonUtils.toJson(chartData));
        report.setSuggestionText(response != null && StringUtils.hasText(response.getSuggestionText())
                ? response.getSuggestionText()
                : String.join("；", nextActions));
        report.setStatus("generated");
        learningReportMapper.insert(report);
        return toVO(report);
    }

    @Override
    public LearningReportVO detail(Long id) {
        return toVO(getOwnedReport(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        learningReportMapper.deleteById(getOwnedReport(id));
    }

    @Override
    public String exportMarkdown(Long id) {
        LearningReport report = getOwnedReport(id);
        JsonNode data = parseJson(report.getReportJson());
        return "# " + report.getTitle()
                + "\n\n## 摘要\n" + report.getSummary()
                + "\n\n## 学习足迹\n" + markdownActivity(data)
                + "\n\n## 路径进度\n" + markdownPathProgress(data)
                + "\n\n## 测验表现\n" + markdownQuizPerformance(data)
                + "\n\n## 薄弱知识点\n" + markdownList(data, "weak_points", "暂无明显薄弱点")
                + "\n\n## 优势知识点\n" + markdownList(data, "strengths", "暂无足够数据")
                + "\n\n## 下一步计划\n" + markdownList(data, "next_actions", report.getSuggestionText())
                + "\n\n## AI 学习建议\n" + report.getSuggestionText();
    }

    private Map<String, Object> buildLearningEvidence(Long userId, Long spaceId, ReportOverviewVO overview) {
        List<GeneratedResource> resources = generatedResourceMapper.selectList(new LambdaQueryWrapper<GeneratedResource>()
                .eq(GeneratedResource::getUserId, userId)
                .eq(GeneratedResource::getSpaceId, spaceId)
                .eq(GeneratedResource::getDeleted, 0)
                .orderByDesc(GeneratedResource::getCreatedAt));
        List<Quiz> quizzes = quizMapper.selectList(baseQuiz(userId, spaceId).orderByDesc(Quiz::getCreatedAt));
        List<Long> quizIds = quizzes.stream().map(Quiz::getId).toList();
        List<QuizAnswer> answers = quizIds.isEmpty() ? List.of() : quizAnswerMapper.selectList(new LambdaQueryWrapper<QuizAnswer>()
                .eq(QuizAnswer::getUserId, userId)
                .in(QuizAnswer::getQuizId, quizIds)
                .eq(QuizAnswer::getDeleted, 0));
        List<LearningPath> paths = learningPathMapper.selectList(new LambdaQueryWrapper<LearningPath>()
                .eq(LearningPath::getUserId, userId)
                .eq(LearningPath::getSpaceId, spaceId)
                .eq(LearningPath::getDeleted, 0));
        List<LearningPathItem> pathItems = learningPathItemMapper.selectList(new LambdaQueryWrapper<LearningPathItem>()
                .eq(LearningPathItem::getUserId, userId)
                .eq(LearningPathItem::getSpaceId, spaceId)
                .eq(LearningPathItem::getDeleted, 0)
                .orderByAsc(LearningPathItem::getDueDate));
        List<Conversation> conversations = conversationMapper.selectList(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getUserId, userId)
                .eq(Conversation::getSpaceId, spaceId)
                .eq(Conversation::getDeleted, 0));
        List<KnowledgeFile> knowledgeFiles = knowledgeFileMapper.selectList(new LambdaQueryWrapper<KnowledgeFile>()
                .eq(KnowledgeFile::getUserId, userId)
                .eq(KnowledgeFile::getSpaceId, spaceId)
                .eq(KnowledgeFile::getDeleted, 0)
                .orderByDesc(KnowledgeFile::getCreatedAt));

        Map<String, Long> resourceBreakdown = resources.stream()
                .collect(Collectors.groupingBy(
                        item -> StringUtils.hasText(item.getResourceType()) ? item.getResourceType() : "other",
                        LinkedHashMap::new,
                        Collectors.counting()));
        Map<Long, BigDecimal> earnedByQuiz = answers.stream().collect(Collectors.groupingBy(
                QuizAnswer::getQuizId,
                Collectors.mapping(answer -> answer.getScore() == null ? BigDecimal.ZERO : answer.getScore(),
                        Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
        long completedItems = pathItems.stream().filter(item -> "completed".equalsIgnoreCase(item.getStatus())).count();
        long submittedQuizCount = quizzes.stream().filter(quiz -> earnedByQuiz.containsKey(quiz.getId())).count();
        long correctAnswers = answers.stream().filter(answer -> Boolean.TRUE.equals(answer.getIsCorrect())).count();

        Map<String, Object> activitySummary = new LinkedHashMap<>();
        activitySummary.put("conversation_count", conversations.size());
        activitySummary.put("message_count", conversations.stream().mapToInt(item -> item.getMessageCount() == null ? 0 : item.getMessageCount()).sum());
        activitySummary.put("knowledge_file_count", knowledgeFiles.size());
        activitySummary.put("knowledge_chunk_count", knowledgeFiles.stream().mapToInt(item -> item.getChunkCount() == null ? 0 : item.getChunkCount()).sum());
        activitySummary.put("resource_count", resources.size());
        activitySummary.put("path_count", paths.size());
        activitySummary.put("quiz_count", quizzes.size());
        activitySummary.put("submitted_quiz_count", submittedQuizCount);

        Map<String, Object> pathProgress = new LinkedHashMap<>();
        pathProgress.put("path_count", paths.size());
        pathProgress.put("average_progress", avg(paths.stream().map(LearningPath::getProgressRate).toList()));
        pathProgress.put("item_count", pathItems.size());
        pathProgress.put("completed_item_count", completedItems);
        pathProgress.put("pending_item_count", Math.max(0, pathItems.size() - completedItems));
        pathProgress.put("next_items", pathItems.stream()
                .filter(item -> !"completed".equalsIgnoreCase(item.getStatus()))
                .limit(5)
                .map(item -> {
                    Map<String, Object> value = new LinkedHashMap<>();
                    value.put("title", item.getTitle());
                    value.put("estimated_minutes", item.getEstimatedMinutes());
                    value.put("due_date", item.getDueDate());
                    return value;
                }).toList());

        Map<String, Object> quizPerformance = new LinkedHashMap<>();
        quizPerformance.put("quiz_count", quizzes.size());
        quizPerformance.put("submitted_quiz_count", submittedQuizCount);
        quizPerformance.put("average_score", overview.getAverageScore());
        quizPerformance.put("answer_count", answers.size());
        quizPerformance.put("correct_answer_count", correctAnswers);
        quizPerformance.put("wrong_answer_count", Math.max(0, answers.size() - correctAnswers));
        quizPerformance.put("recent_quizzes", quizzes.stream().filter(quiz -> earnedByQuiz.containsKey(quiz.getId())).limit(5).map(quiz -> {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("title", quiz.getTitle());
            value.put("score", earnedByQuiz.get(quiz.getId()));
            value.put("total_score", quiz.getTotalScore());
            value.put("score_rate", rate(earnedByQuiz.get(quiz.getId()), quiz.getTotalScore()).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP));
            return value;
        }).toList());

        Map<String, Object> knowledgeLibrary = new LinkedHashMap<>();
        knowledgeLibrary.put("file_count", knowledgeFiles.size());
        knowledgeLibrary.put("chunk_count", knowledgeFiles.stream().mapToInt(item -> item.getChunkCount() == null ? 0 : item.getChunkCount()).sum());
        knowledgeLibrary.put("recent_files", knowledgeFiles.stream().limit(5).map(KnowledgeFile::getOriginalName).toList());

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("activity_summary", activitySummary);
        evidence.put("resource_breakdown", resourceBreakdown);
        evidence.put("path_progress", pathProgress);
        evidence.put("quiz_performance", quizPerformance);
        evidence.put("knowledge_library", knowledgeLibrary);
        evidence.put("recent_outputs", resources.stream().limit(5).map(item -> {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("title", item.getTitle());
            value.put("resource_type", item.getResourceType());
            value.put("subject", item.getSubject());
            return value;
        }).toList());
        return evidence;
    }

    private List<String> buildLearningObservations(Map<String, Object> evidence) {
        Map<?, ?> activity = (Map<?, ?>) evidence.getOrDefault("activity_summary", Map.of());
        Map<?, ?> path = (Map<?, ?>) evidence.getOrDefault("path_progress", Map.of());
        Map<?, ?> quiz = (Map<?, ?>) evidence.getOrDefault("quiz_performance", Map.of());
        List<String> observations = new ArrayList<>();
        observations.add("本空间已形成 " + mapValue(activity, "resource_count") + " 份可复习资源和 "
                + mapValue(activity, "knowledge_file_count") + " 份知识库资料。");
        observations.add("学习路径已完成 " + mapValue(path, "completed_item_count") + " / "
                + mapValue(path, "item_count") + " 个任务。");
        observations.add("已完成 " + mapValue(quiz, "submitted_quiz_count") + " 次测验，平均得分率为 "
                + mapValue(quiz, "average_score") + "% 。");
        return observations;
    }

    private Object mapValue(Map<?, ?> values, String key) {
        Object value = values.get(key);
        return value == null ? 0 : value;
    }

    private CompanionRole resolveRole(Boolean enabled, Long roleId) {
        if (!Boolean.TRUE.equals(enabled)) {
            return null;
        }
        return companionRoleService.getOwnedActiveRole(roleId);
    }

    private LearningReport getOwnedReport(Long id) {
        LearningReport report = learningReportMapper.selectOne(new LambdaQueryWrapper<LearningReport>()
                .eq(LearningReport::getId, id)
                .eq(LearningReport::getUserId, LoginUserHolder.requireCurrentUserId())
                .eq(LearningReport::getDeleted, 0));
        if (report == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "学习报告不存在或无权访问");
        }
        return report;
    }

    private LambdaQueryWrapper<Quiz> baseQuiz(Long userId, Long spaceId) {
        return new LambdaQueryWrapper<Quiz>()
                .eq(Quiz::getUserId, userId)
                .eq(Quiz::getDeleted, 0)
                .eq(spaceId != null, Quiz::getSpaceId, spaceId);
    }

    private BigDecimal avg(List<BigDecimal> values) {
        List<BigDecimal> effective = values.stream().filter(value -> value != null).toList();
        if (effective.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = effective.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(effective.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal rate(BigDecimal score, BigDecimal total) {
        return total == null || total.compareTo(BigDecimal.ZERO) <= 0
                ? BigDecimal.ZERO
                : score.divide(total, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal averageQuizScore(Long userId, List<Quiz> quizzes) {
        if (quizzes.isEmpty()) {
            return BigDecimal.ZERO;
        }
        List<Long> quizIds = quizzes.stream().map(Quiz::getId).toList();
        Map<Long, BigDecimal> earned = quizAnswerMapper.selectList(new LambdaQueryWrapper<QuizAnswer>()
                        .eq(QuizAnswer::getUserId, userId)
                        .in(QuizAnswer::getQuizId, quizIds)
                        .eq(QuizAnswer::getDeleted, 0))
                .stream()
                .collect(Collectors.groupingBy(QuizAnswer::getQuizId,
                        Collectors.mapping(answer -> answer.getScore() == null ? BigDecimal.ZERO : answer.getScore(),
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
        List<BigDecimal> percentages = quizzes.stream()
                .filter(quiz -> quiz.getTotalScore() != null && quiz.getTotalScore().compareTo(BigDecimal.ZERO) > 0)
                .filter(quiz -> earned.containsKey(quiz.getId()))
                .map(quiz -> earned.get(quiz.getId())
                        .multiply(BigDecimal.valueOf(100))
                        .divide(quiz.getTotalScore(), 2, RoundingMode.HALF_UP))
                .toList();
        return avg(percentages);
    }

    private Map<String, Object> overviewPayload(ReportOverviewVO overview) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("space_count", overview.getSpaceCount());
        payload.put("resource_count", overview.getResourceCount());
        payload.put("quiz_count", overview.getQuizCount());
        payload.put("path_count", overview.getPathCount());
        payload.put("average_score", overview.getAverageScore());
        payload.put("average_mastery", overview.getAverageMastery());
        return payload;
    }

    private List<String> buildNextActions(ReportOverviewVO overview, List<String> weakPoints) {
        List<String> actions = new ArrayList<>();
        if (!weakPoints.isEmpty()) {
            actions.add("优先复习“" + String.join("、", weakPoints.stream().limit(3).toList()) + "”，并完成一组针对性选择题。");
        }
        if (overview.getResourceCount() == 0) {
            actions.add("先生成一份本空间的核心知识点讲义，建立可复习的材料基础。");
        }
        if (overview.getQuizCount() == 0) {
            actions.add("完成一次 5 题阶段测验，让系统形成首批掌握度数据。");
        }
        if (overview.getPathCount() == 0) {
            actions.add("根据当前目标生成学习路径，把复习内容拆成每天可完成的任务。");
        }
        if (actions.isEmpty()) {
            actions.add("按当前学习路径继续推进，并在完成下一阶段后重新生成报告。");
        }
        return actions;
    }

    private String buildSummary(ReportOverviewVO overview, List<String> weakPoints) {
        String weakness = weakPoints.isEmpty() ? "当前还没有形成明显薄弱点" : "当前需要重点关注 " + String.join("、", weakPoints);
        return "本空间已有 " + overview.getResourceCount() + " 份资源、" + overview.getQuizCount()
                + " 次测验和 " + overview.getPathCount() + " 条学习路径；" + weakness + "。";
    }

    private String markdownList(JsonNode data, String field, String fallback) {
        if (data != null && data.path(field).isArray() && !data.path(field).isEmpty()) {
            List<String> values = new ArrayList<>();
            data.path(field).forEach(item -> values.add("- " + item.asText()));
            return String.join("\n", values);
        }
        return "- " + (StringUtils.hasText(fallback) ? fallback : "暂无");
    }

    private String markdownActivity(JsonNode data) {
        JsonNode value = data == null ? null : data.path("activity_summary");
        if (value == null || value.isMissingNode()) {
            return "- 暂无可汇总的学习行为";
        }
        return "- AI 对话：" + value.path("conversation_count").asInt(0) + " 次，共 " + value.path("message_count").asInt(0) + " 条消息"
                + "\n- 知识库：" + value.path("knowledge_file_count").asInt(0) + " 份资料，共 " + value.path("knowledge_chunk_count").asInt(0) + " 个片段"
                + "\n- 已生成资源：" + value.path("resource_count").asInt(0) + " 份"
                + "\n- 已完成测验：" + value.path("submitted_quiz_count").asInt(0) + " 次";
    }

    private String markdownPathProgress(JsonNode data) {
        JsonNode value = data == null ? null : data.path("path_progress");
        if (value == null || value.isMissingNode()) {
            return "- 暂无学习路径数据";
        }
        return "- 平均进度：" + value.path("average_progress").asText("0") + "%"
                + "\n- 已完成任务：" + value.path("completed_item_count").asInt(0) + " / " + value.path("item_count").asInt(0)
                + "\n- 待完成任务：" + value.path("pending_item_count").asInt(0);
    }

    private String markdownQuizPerformance(JsonNode data) {
        JsonNode value = data == null ? null : data.path("quiz_performance");
        if (value == null || value.isMissingNode()) {
            return "- 暂无测验数据";
        }
        return "- 已完成测验：" + value.path("submitted_quiz_count").asInt(0) + " 次"
                + "\n- 平均得分率：" + value.path("average_score").asText("0") + "%"
                + "\n- 作答统计：" + value.path("correct_answer_count").asInt(0) + " 题正确，"
                + value.path("wrong_answer_count").asInt(0) + " 题需复盘";
    }

    private LearningReportVO toVO(LearningReport report) {
        return LearningReportVO.builder()
                .id(report.getId())
                .userId(report.getUserId())
                .spaceId(report.getSpaceId())
                .reportType(report.getReportType())
                .title(report.getTitle())
                .summary(report.getSummary())
                .reportJson(parseJson(report.getReportJson()))
                .chartDataJson(parseJson(report.getChartDataJson()))
                .suggestionText(report.getSuggestionText())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .build();
    }

    private JsonNode parseJson(String json) {
        return StringUtils.hasText(json) ? JsonUtils.fromJson(json, JsonNode.class) : null;
    }
}
