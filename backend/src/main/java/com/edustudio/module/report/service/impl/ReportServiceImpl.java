package com.edustudio.module.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edustudio.common.api.ResultCode;
import com.edustudio.common.exception.BusinessException;
import com.edustudio.common.security.LoginUserHolder;
import com.edustudio.common.utils.JsonUtils;
import com.edustudio.integration.ai.AiServiceClient;
import com.edustudio.integration.ai.dto.AiReportGenerateRequest;
import com.edustudio.integration.ai.dto.AiReportGenerateResponse;
import com.edustudio.module.learningpath.entity.LearningPath;
import com.edustudio.module.learningpath.mapper.LearningPathMapper;
import com.edustudio.module.learningspace.entity.LearningSpace;
import com.edustudio.module.learningspace.mapper.LearningSpaceMapper;
import com.edustudio.module.learningspace.service.LearningSpaceService;
import com.edustudio.module.modelprovider.service.ModelProviderService;
import com.edustudio.module.profile.entity.MasteryRecord;
import com.edustudio.module.profile.mapper.MasteryRecordMapper;
import com.edustudio.module.quiz.entity.Quiz;
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

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final LearningReportMapper learningReportMapper;
    private final LearningSpaceMapper learningSpaceMapper;
    private final GeneratedResourceMapper generatedResourceMapper;
    private final QuizMapper quizMapper;
    private final LearningPathMapper learningPathMapper;
    private final MasteryRecordMapper masteryRecordMapper;
    private final LearningSpaceService learningSpaceService;
    private final ModelProviderService modelProviderService;
    private final AiServiceClient aiServiceClient;

    @Override
    public ReportOverviewVO overview() {
        Long userId = LoginUserHolder.requireCurrentUserId();
        List<Quiz> quizzes = quizMapper.selectList(baseQuiz(userId, null));
        List<MasteryRecord> mastery = masteryRecordMapper.selectList(new LambdaQueryWrapper<MasteryRecord>()
                .eq(MasteryRecord::getUserId, userId)
                .eq(MasteryRecord::getDeleted, 0));
        return ReportOverviewVO.builder()
                .spaceCount(learningSpaceMapper.selectCount(new LambdaQueryWrapper<LearningSpace>().eq(LearningSpace::getUserId, userId).eq(LearningSpace::getDeleted, 0)))
                .resourceCount(generatedResourceMapper.selectCount(new LambdaQueryWrapper<GeneratedResource>().eq(GeneratedResource::getUserId, userId).eq(GeneratedResource::getDeleted, 0)))
                .quizCount((long) quizzes.size())
                .pathCount(learningPathMapper.selectCount(new LambdaQueryWrapper<LearningPath>().eq(LearningPath::getUserId, userId).eq(LearningPath::getDeleted, 0)))
                .averageScore(avg(quizzes.stream().map(Quiz::getTotalScore).toList()))
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
        ReportOverviewVO overview = overview();
        List<Map<String, Object>> masteryRecords = masteryRecordMapper.selectList(new LambdaQueryWrapper<MasteryRecord>()
                        .eq(MasteryRecord::getUserId, userId)
                        .eq(MasteryRecord::getSpaceId, request.getSpaceId())
                        .eq(MasteryRecord::getDeleted, 0)
                .orderByAsc(MasteryRecord::getMasteryLevel))
                .stream()
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
        AiReportGenerateResponse response = aiServiceClient.generateReport(AiReportGenerateRequest.builder()
                .modelConfig(modelProviderService.resolveConfig(providerId))
                .reportType(request.getReportType())
                .title(request.getTitle())
                .overview(JsonUtils.fromJson(JsonUtils.toJson(overview), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}))
                .masteryRecords(masteryRecords)
                .build());
        LearningReport report = new LearningReport();
        report.setUserId(userId);
        report.setSpaceId(request.getSpaceId());
        report.setReportType(request.getReportType());
        report.setTitle(response.getTitle());
        report.setSummary(response.getSummary());
        report.setReportJson(response.getReportJson() == null ? "{}" : JsonUtils.toJson(response.getReportJson()));
        report.setChartDataJson(response.getChartDataJson() == null ? "{}" : JsonUtils.toJson(response.getChartDataJson()));
        report.setSuggestionText(response.getSuggestionText());
        report.setStatus("generated");
        learningReportMapper.insert(report);
        return toVO(report);
    }

    @Override
    public LearningReportVO detail(Long id) {
        return toVO(getOwnedReport(id));
    }

    @Override
    public String exportMarkdown(Long id) {
        LearningReport report = getOwnedReport(id);
        return "# " + report.getTitle() + "\n\n## 摘要\n" + report.getSummary() + "\n\n## 建议\n" + report.getSuggestionText();
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
