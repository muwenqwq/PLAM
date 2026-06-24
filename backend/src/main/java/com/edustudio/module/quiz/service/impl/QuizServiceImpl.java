package com.edustudio.module.quiz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edustudio.common.api.PageResult;
import com.edustudio.common.api.ResultCode;
import com.edustudio.common.exception.BusinessException;
import com.edustudio.common.security.LoginUserHolder;
import com.edustudio.common.utils.JsonUtils;
import com.edustudio.integration.ai.AiServiceClient;
import com.edustudio.integration.ai.dto.AiQuizAnalyzeRequest;
import com.edustudio.integration.ai.dto.AiQuizGenerateRequest;
import com.edustudio.integration.ai.dto.AiQuizGenerateResponse;
import com.edustudio.integration.ai.dto.AiQuizQuestionDTO;
import com.edustudio.module.learningspace.service.LearningSpaceService;
import com.edustudio.module.modelprovider.service.ModelProviderService;
import com.edustudio.module.profile.entity.MasteryRecord;
import com.edustudio.module.profile.mapper.MasteryRecordMapper;
import com.edustudio.module.profile.vo.MasteryRecordVO;
import com.edustudio.module.quiz.dto.QuizAnswerSubmitDTO;
import com.edustudio.module.quiz.dto.QuizGenerateRequest;
import com.edustudio.module.quiz.dto.QuizQueryRequest;
import com.edustudio.module.quiz.dto.QuizSubmitRequest;
import com.edustudio.module.quiz.entity.Quiz;
import com.edustudio.module.quiz.entity.QuizAnswer;
import com.edustudio.module.quiz.entity.QuizQuestion;
import com.edustudio.module.quiz.mapper.QuizAnswerMapper;
import com.edustudio.module.quiz.mapper.QuizMapper;
import com.edustudio.module.quiz.mapper.QuizQuestionMapper;
import com.edustudio.module.quiz.service.QuizService;
import com.edustudio.module.quiz.vo.QuizQuestionVO;
import com.edustudio.module.quiz.vo.QuizResultVO;
import com.edustudio.module.quiz.vo.QuizVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QuizServiceImpl implements QuizService {

    private final QuizMapper quizMapper;
    private final QuizQuestionMapper quizQuestionMapper;
    private final QuizAnswerMapper quizAnswerMapper;
    private final MasteryRecordMapper masteryRecordMapper;
    private final LearningSpaceService learningSpaceService;
    private final ModelProviderService modelProviderService;
    private final AiServiceClient aiServiceClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuizVO generate(QuizGenerateRequest request) {
        Long userId = LoginUserHolder.requireCurrentUserId();
        learningSpaceService.assertOwned(request.getSpaceId());
        Long providerId = modelProviderService.resolveProviderId(request.getModelProviderId());
        AiQuizGenerateResponse response = aiServiceClient.generateQuiz(AiQuizGenerateRequest.builder()
                .modelConfig(modelProviderService.resolveConfig(providerId))
                .subject(request.getSubject())
                .title(request.getTitle())
                .knowledgePoints(request.getKnowledgePoints())
                .questionCount(request.getQuestionCount())
                .difficulty(request.getDifficulty())
                .questionType(request.getQuestionType())
                .build());
        Quiz quiz = new Quiz();
        quiz.setUserId(userId);
        quiz.setSpaceId(request.getSpaceId());
        quiz.setResourceId(request.getResourceId());
        quiz.setTitle(response.getTitle());
        quiz.setSubject(response.getSubject());
        quiz.setDifficulty(response.getDifficulty());
        quiz.setQuestionCount(response.getQuestions() == null ? 0 : response.getQuestions().size());
        quiz.setTotalScore(response.getTotalScore());
        quiz.setStatus("published");
        quizMapper.insert(quiz);
        for (AiQuizQuestionDTO dto : nullSafe(response.getQuestions())) {
            QuizQuestion question = new QuizQuestion();
            question.setQuizId(quiz.getId());
            question.setUserId(userId);
            question.setQuestionOrder(dto.getQuestionOrder());
            question.setQuestionType(dto.getQuestionType());
            question.setStem(dto.getStem());
            question.setOptionsJson(dto.getOptions() == null ? "[]" : JsonUtils.toJson(dto.getOptions()));
            question.setAnswerText(dto.getAnswerText());
            question.setAnalysisText(dto.getAnalysisText());
            question.setKnowledgePoints(dto.getKnowledgePoints() == null ? "[]" : JsonUtils.toJson(dto.getKnowledgePoints()));
            question.setDifficulty(dto.getDifficulty());
            question.setScore(dto.getScore());
            question.setStatus("active");
            quizQuestionMapper.insert(question);
        }
        return detail(quiz.getId());
    }

    @Override
    public PageResult<QuizVO> page(QuizQueryRequest request) {
        Long userId = LoginUserHolder.requireCurrentUserId();
        LambdaQueryWrapper<Quiz> wrapper = new LambdaQueryWrapper<Quiz>()
                .eq(Quiz::getUserId, userId)
                .eq(Quiz::getDeleted, 0)
                .eq(request.getSpaceId() != null, Quiz::getSpaceId, request.getSpaceId())
                .eq(StringUtils.hasText(request.getStatus()), Quiz::getStatus, request.getStatus())
                .like(StringUtils.hasText(request.getKeyword()), Quiz::getTitle, request.getKeyword())
                .orderByDesc(Quiz::getCreatedAt);
        Page<Quiz> result = quizMapper.selectPage(new Page<>(request.getPageNum(), request.getPageSize()), wrapper);
        return PageResult.of(result.getRecords().stream().map(quiz -> toVO(quiz, false)).toList(), result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public QuizVO detail(Long id) {
        return toVO(getOwnedQuiz(id), true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuizResultVO submit(Long id, QuizSubmitRequest request) {
        Quiz quiz = getOwnedQuiz(id);
        Map<Long, String> answers = request.getAnswers().stream().collect(java.util.stream.Collectors.toMap(QuizAnswerSubmitDTO::getQuestionId, QuizAnswerSubmitDTO::getAnswerText, (a, b) -> b));
        BigDecimal score = BigDecimal.ZERO;
        LinkedHashSet<String> weakPoints = new LinkedHashSet<>();
        for (QuizQuestion question : questions(quiz.getId())) {
            String answerText = answers.getOrDefault(question.getId(), "");
            BigDecimal earned = grade(question, answerText);
            score = score.add(earned);
            if (earned.compareTo(question.getScore().multiply(BigDecimal.valueOf(0.6))) < 0) {
                weakPoints.addAll(parseList(question.getKnowledgePoints()));
            }
            upsertAnswer(quiz, question, answerText, earned);
            updateMastery(quiz, question, earned);
        }
        quiz.setStatus("submitted");
        quizMapper.updateById(quiz);
        var analysis = aiServiceClient.analyzeQuiz(AiQuizAnalyzeRequest.builder()
                .modelConfig(modelProviderService.resolveConfig(null))
                .quizTitle(quiz.getTitle())
                .score(score)
                .totalScore(quiz.getTotalScore())
                .weakPoints(weakPoints.stream().toList())
                .answers(Collections.emptyList())
                .build());
        return QuizResultVO.builder()
                .quizId(quiz.getId())
                .score(score)
                .totalScore(quiz.getTotalScore())
                .accuracyRate(rate(score, quiz.getTotalScore()))
                .weakPoints(weakPoints.stream().toList())
                .analysisMarkdown(analysis.getAnalysisMarkdown())
                .build();
    }

    @Override
    public QuizResultVO result(Long id) {
        Quiz quiz = getOwnedQuiz(id);
        BigDecimal score = answers(quiz.getId()).stream().map(QuizAnswer::getScore).reduce(BigDecimal.ZERO, BigDecimal::add);
        return QuizResultVO.builder()
                .quizId(quiz.getId())
                .score(score)
                .totalScore(quiz.getTotalScore())
                .accuracyRate(rate(score, quiz.getTotalScore()))
                .weakPoints(Collections.emptyList())
                .analysisMarkdown("已根据当前保存答案生成测验结果。")
                .build();
    }

    @Override
    public QuizResultVO analysis(Long id) {
        return result(id);
    }

    @Override
    public List<MasteryRecordVO> mastery() {
        Long userId = LoginUserHolder.requireCurrentUserId();
        return masteryRecordMapper.selectList(new LambdaQueryWrapper<MasteryRecord>()
                        .eq(MasteryRecord::getUserId, userId)
                        .eq(MasteryRecord::getDeleted, 0)
                        .orderByAsc(MasteryRecord::getMasteryLevel))
                .stream().map(this::toMasteryVO).toList();
    }

    private BigDecimal grade(QuizQuestion question, String answerText) {
        if (!StringUtils.hasText(answerText)) {
            return BigDecimal.ZERO;
        }
        String normalized = answerText.trim();
        if ("single_choice".equals(question.getQuestionType()) || "judge".equals(question.getQuestionType())) {
            return normalized.equalsIgnoreCase(question.getAnswerText().trim()) ? question.getScore() : BigDecimal.ZERO;
        }
        List<String> keywords = List.of(question.getAnswerText().split("\\s+"));
        long matched = keywords.stream().filter(keyword -> StringUtils.hasText(keyword) && normalized.contains(keyword)).count();
        BigDecimal ratio = keywords.isEmpty() ? BigDecimal.valueOf(0.6) : BigDecimal.valueOf(matched).divide(BigDecimal.valueOf(keywords.size()), 2, RoundingMode.HALF_UP);
        return question.getScore().multiply(ratio.max(BigDecimal.valueOf(0.4))).setScale(2, RoundingMode.HALF_UP);
    }

    private void upsertAnswer(Quiz quiz, QuizQuestion question, String answerText, BigDecimal earned) {
        QuizAnswer answer = quizAnswerMapper.selectOne(new LambdaQueryWrapper<QuizAnswer>()
                .eq(QuizAnswer::getQuizId, quiz.getId())
                .eq(QuizAnswer::getQuestionId, question.getId())
                .eq(QuizAnswer::getUserId, quiz.getUserId())
                .eq(QuizAnswer::getDeleted, 0));
        if (answer == null) {
            answer = new QuizAnswer();
            answer.setQuizId(quiz.getId());
            answer.setQuestionId(question.getId());
            answer.setUserId(quiz.getUserId());
        }
        answer.setAnswerText(answerText);
        answer.setScore(earned);
        answer.setIsCorrect(earned.compareTo(question.getScore()) >= 0);
        answer.setFeedbackText(question.getAnalysisText());
        answer.setSubmittedAt(LocalDateTime.now());
        answer.setStatus("submitted");
        if (answer.getId() == null) {
            quizAnswerMapper.insert(answer);
        } else {
            quizAnswerMapper.updateById(answer);
        }
    }

    private void updateMastery(Quiz quiz, QuizQuestion question, BigDecimal earned) {
        BigDecimal mastery = rate(earned, question.getScore()).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
        for (String point : parseList(question.getKnowledgePoints())) {
            MasteryRecord record = masteryRecordMapper.selectOne(new LambdaQueryWrapper<MasteryRecord>()
                    .eq(MasteryRecord::getUserId, quiz.getUserId())
                    .eq(MasteryRecord::getSpaceId, quiz.getSpaceId())
                    .eq(MasteryRecord::getKnowledgePoint, point)
                    .eq(MasteryRecord::getDeleted, 0));
            if (record == null) {
                record = new MasteryRecord();
                record.setUserId(quiz.getUserId());
                record.setSpaceId(quiz.getSpaceId());
                record.setKnowledgePoint(point);
                record.setSubject(quiz.getSubject());
                record.setReviewCount(0);
            }
            record.setMasteryLevel(mastery);
            record.setWeaknessLevel(BigDecimal.valueOf(100).subtract(mastery));
            record.setLastQuizId(quiz.getId());
            record.setLastScore(earned);
            record.setReviewCount((record.getReviewCount() == null ? 0 : record.getReviewCount()) + 1);
            record.setStatus(mastery.compareTo(BigDecimal.valueOf(80)) >= 0 ? "mastered" : mastery.compareTo(BigDecimal.valueOf(60)) >= 0 ? "learning" : "weak");
            if (record.getId() == null) {
                masteryRecordMapper.insert(record);
            } else {
                masteryRecordMapper.updateById(record);
            }
        }
    }

    private Quiz getOwnedQuiz(Long id) {
        Quiz quiz = quizMapper.selectOne(new LambdaQueryWrapper<Quiz>()
                .eq(Quiz::getId, id)
                .eq(Quiz::getUserId, LoginUserHolder.requireCurrentUserId())
                .eq(Quiz::getDeleted, 0));
        if (quiz == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "测验不存在或无权访问");
        }
        return quiz;
    }

    private List<QuizQuestion> questions(Long quizId) {
        return quizQuestionMapper.selectList(new LambdaQueryWrapper<QuizQuestion>()
                .eq(QuizQuestion::getQuizId, quizId)
                .eq(QuizQuestion::getUserId, LoginUserHolder.requireCurrentUserId())
                .eq(QuizQuestion::getDeleted, 0)
                .orderByAsc(QuizQuestion::getQuestionOrder));
    }

    private List<QuizAnswer> answers(Long quizId) {
        return quizAnswerMapper.selectList(new LambdaQueryWrapper<QuizAnswer>()
                .eq(QuizAnswer::getQuizId, quizId)
                .eq(QuizAnswer::getUserId, LoginUserHolder.requireCurrentUserId())
                .eq(QuizAnswer::getDeleted, 0));
    }

    private QuizVO toVO(Quiz quiz, boolean includeQuestions) {
        return QuizVO.builder()
                .id(quiz.getId())
                .userId(quiz.getUserId())
                .spaceId(quiz.getSpaceId())
                .resourceId(quiz.getResourceId())
                .title(quiz.getTitle())
                .subject(quiz.getSubject())
                .difficulty(quiz.getDifficulty())
                .questionCount(quiz.getQuestionCount())
                .totalScore(quiz.getTotalScore())
                .status(quiz.getStatus())
                .createdAt(quiz.getCreatedAt())
                .questions(includeQuestions ? questions(quiz.getId()).stream().map(this::toQuestionVO).toList() : Collections.emptyList())
                .build();
    }

    private QuizQuestionVO toQuestionVO(QuizQuestion question) {
        return QuizQuestionVO.builder()
                .id(question.getId())
                .questionOrder(question.getQuestionOrder())
                .questionType(question.getQuestionType())
                .stem(question.getStem())
                .options(parseJson(question.getOptionsJson()))
                .answerText(question.getAnswerText())
                .analysisText(question.getAnalysisText())
                .knowledgePoints(parseJson(question.getKnowledgePoints()))
                .difficulty(question.getDifficulty())
                .score(question.getScore())
                .build();
    }

    private MasteryRecordVO toMasteryVO(MasteryRecord record) {
        return MasteryRecordVO.builder()
                .id(record.getId())
                .spaceId(record.getSpaceId())
                .knowledgePoint(record.getKnowledgePoint())
                .subject(record.getSubject())
                .masteryLevel(record.getMasteryLevel())
                .weaknessLevel(record.getWeaknessLevel())
                .lastQuizId(record.getLastQuizId())
                .lastScore(record.getLastScore())
                .reviewCount(record.getReviewCount())
                .status(record.getStatus())
                .build();
    }

    private BigDecimal rate(BigDecimal score, BigDecimal total) {
        return total == null || total.compareTo(BigDecimal.ZERO) <= 0 ? BigDecimal.ZERO : score.divide(total, 4, RoundingMode.HALF_UP);
    }

    private List<String> parseList(String json) {
        return StringUtils.hasText(json) ? JsonUtils.fromJson(json, new TypeReference<List<String>>() {}) : Collections.emptyList();
    }

    private JsonNode parseJson(String json) {
        return StringUtils.hasText(json) ? JsonUtils.fromJson(json, JsonNode.class) : null;
    }

    private <T> List<T> nullSafe(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }
}
