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
import com.edustudio.module.companion.entity.CompanionRole;
import com.edustudio.module.companion.service.CompanionRoleService;
import com.edustudio.module.companion.support.CompanionRolePayload;
import com.edustudio.module.learningspace.service.LearningSpaceService;
import com.edustudio.module.knowledge.service.KnowledgeService;
import com.edustudio.module.modelprovider.service.ModelProviderService;
import com.edustudio.module.profile.entity.MasteryRecord;
import com.edustudio.module.profile.mapper.MasteryRecordMapper;
import com.edustudio.module.profile.service.UserProfileService;
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
import com.edustudio.module.quiz.vo.QuizQuestionFeedbackVO;
import com.edustudio.module.quiz.vo.QuizQuestionVO;
import com.edustudio.module.quiz.vo.QuizResultVO;
import com.edustudio.module.quiz.vo.QuizVO;
import com.edustudio.module.resource.entity.GeneratedResource;
import com.edustudio.module.resource.mapper.GeneratedResourceMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizServiceImpl implements QuizService {

    private final QuizMapper quizMapper;
    private final QuizQuestionMapper quizQuestionMapper;
    private final QuizAnswerMapper quizAnswerMapper;
    private final MasteryRecordMapper masteryRecordMapper;
    private final LearningSpaceService learningSpaceService;
    private final ModelProviderService modelProviderService;
    private final AiServiceClient aiServiceClient;
    private final CompanionRoleService companionRoleService;
    private final UserProfileService userProfileService;
    private final GeneratedResourceMapper generatedResourceMapper;
    private final KnowledgeService knowledgeService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuizVO generate(QuizGenerateRequest request) {
        Long userId = LoginUserHolder.requireCurrentUserId();
        learningSpaceService.assertOwned(request.getSpaceId());
        Long providerId = modelProviderService.resolveProviderId(request.getModelProviderId());
        CompanionRole role = resolveRole(request.getRolePlayEnabled(), request.getCompanionRoleId());
        Map<String, Object> rolePayload = CompanionRolePayload.from(role);
        Map<String, Object> learnerProfile = userProfileService.getAiProfile(request.getSpaceId());
        AiQuizGenerateResponse response = aiServiceClient.generateQuiz(AiQuizGenerateRequest.builder()
                .modelConfig(modelProviderService.resolveConfig(providerId))
                .subject(request.getSubject())
                .title(request.getTitle())
                .knowledgePoints(request.getKnowledgePoints())
                .questionCount(request.getQuestionCount())
                .difficulty(request.getDifficulty())
                .questionType("single_choice")
                .profile(learnerProfile)
                .rolePlayEnabled(role != null)
                .companionRole(rolePayload)
                .build());
        List<AiQuizQuestionDTO> generatedQuestions = normalizeChoiceQuestions(response == null ? null : response.getQuestions());
        if (response == null || generatedQuestions.isEmpty()) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "AI 服务没有返回可用的四选一题目");
        }
        Quiz quiz = new Quiz();
        quiz.setUserId(userId);
        quiz.setSpaceId(request.getSpaceId());
        quiz.setResourceId(request.getResourceId());
        quiz.setTitle(response.getTitle());
        quiz.setSubject(response.getSubject());
        quiz.setDifficulty(response.getDifficulty());
        quiz.setQuestionCount(generatedQuestions.size());
        quiz.setTotalScore(generatedQuestions.stream()
                .map(AiQuizQuestionDTO::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        quiz.setStatus("published");
        quizMapper.insert(quiz);
        for (AiQuizQuestionDTO dto : generatedQuestions) {
            QuizQuestion question = new QuizQuestion();
            question.setQuizId(quiz.getId());
            question.setUserId(userId);
            question.setQuestionOrder(dto.getQuestionOrder());
            question.setQuestionType(dto.getQuestionType());
            question.setStem(dto.getStem());
            question.setOptionsJson(dto.getOptions() == null ? "[]" : JsonUtils.toJson(dto.getOptions()));
            question.setAnswerText(dto.getAnswerText());
            question.setAnalysisText(dto.getAnalysisText());
            question.setOptionAnalysisJson(JsonUtils.toJson(dto.getOptionExplanations()));
            question.setKnowledgePoints(dto.getKnowledgePoints() == null ? "[]" : JsonUtils.toJson(dto.getKnowledgePoints()));
            question.setDifficulty(dto.getDifficulty());
            question.setScore(dto.getScore());
            question.setStatus("active");
            quizQuestionMapper.insert(question);
        }
        GeneratedResource quizResource = createQuizResource(quiz, generatedQuestions, request.getKnowledgePoints());
        quiz.setResourceId(quizResource.getId());
        quizMapper.updateById(quiz);
        knowledgeService.syncGeneratedResource(quizResource);
        learningSpaceService.incrementResourceCount(request.getSpaceId());
        userProfileService.recordActivity(
                request.getSpaceId(), request.getSubject(), request.getKnowledgePoints(), List.of(), "quiz_generation");
        return detail(quiz.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Quiz quiz = getOwnedQuiz(id);
        quizAnswerMapper.delete(new LambdaQueryWrapper<QuizAnswer>()
                .eq(QuizAnswer::getQuizId, quiz.getId())
                .eq(QuizAnswer::getUserId, quiz.getUserId()));
        quizQuestionMapper.delete(new LambdaQueryWrapper<QuizQuestion>()
                .eq(QuizQuestion::getQuizId, quiz.getId())
                .eq(QuizQuestion::getUserId, quiz.getUserId()));
        masteryRecordMapper.delete(new LambdaQueryWrapper<MasteryRecord>()
                .eq(MasteryRecord::getUserId, quiz.getUserId())
                .eq(MasteryRecord::getSpaceId, quiz.getSpaceId())
                .eq(MasteryRecord::getLastQuizId, quiz.getId()));
        if (quiz.getResourceId() != null) {
            GeneratedResource resource = generatedResourceMapper.selectOne(new LambdaQueryWrapper<GeneratedResource>()
                    .eq(GeneratedResource::getId, quiz.getResourceId())
                    .eq(GeneratedResource::getUserId, quiz.getUserId())
                    .eq(GeneratedResource::getDeleted, 0));
            if (resource != null) {
                generatedResourceMapper.deleteById(resource.getId());
                knowledgeService.deleteGeneratedResourceIndex(resource.getId(), resource.getUserId());
            }
        }
        quizMapper.deleteById(quiz);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByResourceId(Long resourceId) {
        if (resourceId == null) {
            return false;
        }
        Quiz quiz = quizMapper.selectOne(new LambdaQueryWrapper<Quiz>()
                .eq(Quiz::getResourceId, resourceId)
                .eq(Quiz::getUserId, LoginUserHolder.requireCurrentUserId())
                .eq(Quiz::getDeleted, 0)
                .last("LIMIT 1"));
        if (quiz == null) {
            return false;
        }
        delete(quiz.getId());
        return true;
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
        Map<Long, String> submittedAnswers = request.getAnswers().stream().collect(Collectors.toMap(QuizAnswerSubmitDTO::getQuestionId, QuizAnswerSubmitDTO::getAnswerText, (a, b) -> b));
        BigDecimal score = BigDecimal.ZERO;
        LinkedHashSet<String> weakPoints = new LinkedHashSet<>();
        List<QuizQuestionFeedbackVO> feedbacks = new ArrayList<>();
        List<Map<String, Object>> answerPayloads = new ArrayList<>();
        for (QuizQuestion question : questions(quiz.getId())) {
            String answerText = submittedAnswers.getOrDefault(question.getId(), "");
            BigDecimal earned = grade(question, answerText);
            score = score.add(earned);
            if (earned.compareTo(question.getScore().multiply(BigDecimal.valueOf(0.6))) < 0) {
                weakPoints.addAll(parseList(question.getKnowledgePoints()));
            }
            QuizQuestionFeedbackVO feedback = feedbackFor(question, answerText, earned);
            feedbacks.add(feedback);
            answerPayloads.add(answerPayload(question, feedback));
            upsertAnswer(quiz, question, answerText, earned, feedback.getFeedback());
            updateMastery(quiz, question, earned);
        }
        quiz.setStatus("submitted");
        quizMapper.updateById(quiz);

        CompanionRole role = resolveRole(request.getRolePlayEnabled(), request.getCompanionRoleId());
        Map<String, Object> rolePayload = CompanionRolePayload.from(role);
        String analysisMarkdown = analyzeWithFallback(quiz, score, weakPoints.stream().toList(), answerPayloads, rolePayload);
        userProfileService.recordActivity(
                quiz.getSpaceId(), quiz.getSubject(), List.of(), weakPoints.stream().toList(), "assessment",
                JsonUtils.toJson(Map.of(
                        "quiz_title", quiz.getTitle(),
                        "score", score,
                        "total_score", quiz.getTotalScore(),
                        "weak_points", weakPoints.stream().toList(),
                        "answered_question_count", feedbacks.size()
                )));
        return QuizResultVO.builder()
                .quizId(quiz.getId())
                .score(score)
                .totalScore(quiz.getTotalScore())
                .accuracyRate(rate(score, quiz.getTotalScore()))
                .weakPoints(weakPoints.stream().toList())
                .analysisMarkdown(analysisMarkdown)
                .questionFeedbacks(feedbacks)
                .build();
    }

    @Override
    public QuizResultVO result(Long id) {
        Quiz quiz = getOwnedQuiz(id);
        Map<Long, QuizAnswer> savedAnswers = answers(quiz.getId()).stream()
                .collect(Collectors.toMap(QuizAnswer::getQuestionId, answer -> answer, (a, b) -> b));
        List<QuizQuestionFeedbackVO> feedbacks = questions(quiz.getId()).stream()
                .map(question -> feedbackFor(question, savedAnswers.get(question.getId())))
                .toList();
        BigDecimal score = savedAnswers.values().stream().map(QuizAnswer::getScore).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<String> weakPoints = feedbacks.stream()
                .filter(item -> item.getScore() != null && item.getFullScore() != null && item.getScore().compareTo(item.getFullScore().multiply(BigDecimal.valueOf(0.6))) < 0)
                .flatMap(item -> nullSafe(item.getKnowledgePoints()).stream())
                .distinct()
                .toList();
        return QuizResultVO.builder()
                .quizId(quiz.getId())
                .score(score)
                .totalScore(quiz.getTotalScore())
                .accuracyRate(rate(score, quiz.getTotalScore()))
                .weakPoints(weakPoints)
                .analysisMarkdown(fallbackAnalysis(quiz, score, weakPoints))
                .questionFeedbacks(feedbacks)
                .build();
    }

    @Override
    public QuizResultVO analysis(Long id) {
        return result(id);
    }

    @Override
    public List<MasteryRecordVO> mastery(Long spaceId) {
        Long userId = LoginUserHolder.requireCurrentUserId();
        if (spaceId != null) {
            learningSpaceService.assertOwned(spaceId);
        }
        return masteryRecordMapper.selectList(new LambdaQueryWrapper<MasteryRecord>()
                        .eq(MasteryRecord::getUserId, userId)
                        .eq(MasteryRecord::getDeleted, 0)
                        .eq(spaceId != null, MasteryRecord::getSpaceId, spaceId)
                        .orderByAsc(MasteryRecord::getMasteryLevel))
                .stream().map(this::toMasteryVO).toList();
    }

    private BigDecimal grade(QuizQuestion question, String answerText) {
        if (!StringUtils.hasText(answerText)) {
            return BigDecimal.ZERO;
        }
        String submitted = normalizeChoiceAnswer(answerText);
        String correct = normalizeChoiceAnswer(question.getAnswerText());
        return submitted.equalsIgnoreCase(correct) ? question.getScore() : BigDecimal.ZERO;
    }

    private void upsertAnswer(Quiz quiz, QuizQuestion question, String answerText, BigDecimal earned, String feedbackText) {
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
        answer.setFeedbackText(feedbackText);
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

    private QuizQuestionFeedbackVO feedbackFor(QuizQuestion question, String answerText, BigDecimal earned) {
        boolean correct = earned.compareTo(question.getScore()) >= 0;
        String feedback;
        if (!StringUtils.hasText(answerText)) {
            feedback = "本题还没有作答，建议先回到对应知识点补一个最小例题。";
        } else {
            feedback = correct ? "选择正确，说明你已经抓住了本题的核心判断。" : "答案不完全正确，先对照解析复盘概念边界。";
        }
        return QuizQuestionFeedbackVO.builder()
                .questionId(question.getId())
                .questionOrder(question.getQuestionOrder())
                .stem(question.getStem())
                .options(parseList(question.getOptionsJson()))
                .answerText(answerText)
                .correctAnswer(question.getAnswerText())
                .score(earned)
                .fullScore(question.getScore())
                .correct(correct)
                .knowledgePoints(parseList(question.getKnowledgePoints()))
                .feedback(feedback)
                .optionExplanations(parseStringMap(question.getOptionAnalysisJson()))
                .build();
    }

    private QuizQuestionFeedbackVO feedbackFor(QuizQuestion question, QuizAnswer answer) {
        String answerText = answer == null ? "" : answer.getAnswerText();
        BigDecimal earned = answer == null || answer.getScore() == null ? BigDecimal.ZERO : answer.getScore();
        QuizQuestionFeedbackVO base = feedbackFor(question, answerText, earned);
        if (answer != null && StringUtils.hasText(answer.getFeedbackText())) {
            base.setFeedback(answer.getFeedbackText());
        }
        return base;
    }

    private Map<String, Object> answerPayload(QuizQuestion question, QuizQuestionFeedbackVO feedback) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("question_id", question.getId());
        payload.put("question_order", question.getQuestionOrder());
        payload.put("question_type", question.getQuestionType());
        payload.put("stem", question.getStem());
        payload.put("options", feedback.getOptions());
        payload.put("student_answer", feedback.getAnswerText());
        payload.put("correct_answer", question.getAnswerText());
        payload.put("score", feedback.getScore());
        payload.put("full_score", feedback.getFullScore());
        payload.put("knowledge_points", feedback.getKnowledgePoints());
        payload.put("rule_feedback", feedback.getFeedback());
        payload.put("option_explanations", feedback.getOptionExplanations());
        return payload;
    }

    private String analyzeWithFallback(Quiz quiz, BigDecimal score, List<String> weakPoints, List<Map<String, Object>> answerPayloads, Map<String, Object> rolePayload) {
        try {
            var analysis = aiServiceClient.analyzeQuiz(AiQuizAnalyzeRequest.builder()
                    .modelConfig(modelProviderService.resolveConfig(null))
                    .quizTitle(quiz.getTitle())
                    .score(score)
                    .totalScore(quiz.getTotalScore())
                    .weakPoints(weakPoints)
                    .answers(answerPayloads)
                    .profile(userProfileService.getAiProfile(quiz.getSpaceId()))
                    .rolePlayEnabled(!rolePayload.isEmpty())
                    .companionRole(rolePayload)
                    .build());
            if (analysis == null || !StringUtils.hasText(analysis.getAnalysisMarkdown())) {
                throw new IllegalStateException("AI 测验分析未返回有效内容");
            }
            return analysis.getAnalysisMarkdown();
        } catch (RuntimeException exception) {
            log.warn("quiz_analysis_fallback quizId={} reason={}", quiz.getId(), exception.getMessage());
            return fallbackAnalysis(quiz, score, weakPoints) + "\n\n> AI 语义分析暂时不可用，已保留规则评分和逐题反馈。";
        }
    }

    private String fallbackAnalysis(Quiz quiz, BigDecimal score, List<String> weakPoints) {
        String weak = weakPoints == null || weakPoints.isEmpty() ? "暂未发现明显薄弱点" : String.join("、", weakPoints);
        return "## 测验反馈\n\n"
                + "- 得分：" + score + " / " + quiz.getTotalScore() + "\n"
                + "- 薄弱点：" + weak + "\n\n"
                + "下一步建议：先复盘低分题，再围绕薄弱点生成一组 5 题小测。";
    }

    private CompanionRole resolveRole(Boolean enabled, Long roleId) {
        if (!Boolean.TRUE.equals(enabled)) {
            return null;
        }
        return companionRoleService.getOwnedActiveRole(roleId);
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
                .optionExplanations(parseJson(question.getOptionAnalysisJson()))
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
        return values == null ? List.of() : values;
    }

    private List<AiQuizQuestionDTO> normalizeChoiceQuestions(List<AiQuizQuestionDTO> questions) {
        List<AiQuizQuestionDTO> normalized = new ArrayList<>();
        int order = 1;
        for (AiQuizQuestionDTO question : nullSafe(questions)) {
            if (question == null || !StringUtils.hasText(question.getStem())) {
                continue;
            }
            List<String> options = normalizeOptions(question.getOptions());
            String answer = normalizeChoiceAnswer(question.getAnswerText());
            if (options.size() != 4 || !List.of("A", "B", "C", "D").contains(answer)) {
                continue;
            }
            question.setQuestionOrder(order++);
            question.setQuestionType("single_choice");
            question.setOptions(options);
            question.setAnswerText(answer);
            question.setAnalysisText(StringUtils.hasText(question.getAnalysisText())
                    ? question.getAnalysisText()
                    : "结合题干概念逐项排除，并说明正确选项成立的条件。");
            question.setOptionExplanations(normalizeOptionExplanations(
                    question.getOptionExplanations(), answer, options, question.getAnalysisText()));
            question.setKnowledgePoints(question.getKnowledgePoints() == null ? List.of() : question.getKnowledgePoints());
            question.setDifficulty(StringUtils.hasText(question.getDifficulty()) ? question.getDifficulty() : "medium");
            question.setScore(question.getScore() == null || question.getScore().compareTo(BigDecimal.ZERO) <= 0
                    ? BigDecimal.TEN
                    : question.getScore());
            normalized.add(question);
        }
        return normalized;
    }

    private List<String> normalizeOptions(List<String> options) {
        if (options == null || options.size() != 4) {
            return List.of();
        }
        List<String> normalized = new ArrayList<>();
        String[] labels = {"A", "B", "C", "D"};
        for (int index = 0; index < labels.length; index++) {
            String value = options.get(index) == null ? "" : options.get(index).trim();
            value = value.replaceFirst("^[A-Da-d][\\.、:：\\)）]\\s*", "");
            if (!StringUtils.hasText(value)) {
                return List.of();
            }
            normalized.add(labels[index] + ". " + value);
        }
        return normalized;
    }

    private String normalizeChoiceAnswer(String answer) {
        if (!StringUtils.hasText(answer)) {
            return "";
        }
        String normalized = answer.trim().toUpperCase();
        return normalized.matches("^[A-D].*") ? normalized.substring(0, 1) : normalized;
    }

    private Map<String, String> normalizeOptionExplanations(Map<String, String> raw, String correctAnswer,
                                                             List<String> options, String analysisText) {
        Map<String, String> explanations = new LinkedHashMap<>();
        String[] labels = {"A", "B", "C", "D"};
        for (int index = 0; index < labels.length; index++) {
            String label = labels[index];
            String explanation = raw == null ? null : raw.get(label);
            if (!StringUtils.hasText(explanation)) {
                String optionText = options.get(index).replaceFirst("^[A-D][.]\\s*", "");
                explanation = label.equals(correctAnswer)
                        ? "正确。“" + optionText + "”符合题干要求。" + analysisText
                        : "错误。“" + optionText + "”不满足题干的关键条件，应与正确选项 " + correctAnswer + " 对照理解。";
            }
            explanations.put(label, explanation.trim());
        }
        return explanations;
    }

    private Map<String, String> parseStringMap(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyMap();
        }
        try {
            return JsonUtils.fromJson(json, new TypeReference<Map<String, String>>() {});
        } catch (RuntimeException ignored) {
            return Collections.emptyMap();
        }
    }

    private GeneratedResource createQuizResource(Quiz quiz, List<AiQuizQuestionDTO> questions,
                                                   List<String> requestedPoints) {
        GeneratedResource resource = new GeneratedResource();
        resource.setUserId(quiz.getUserId());
        resource.setSpaceId(quiz.getSpaceId());
        resource.setResourceType("quiz_set");
        resource.setTitle(quiz.getTitle());
        resource.setSubject(quiz.getSubject());
        List<String> points = new ArrayList<>();
        if (requestedPoints != null) {
            requestedPoints.stream().filter(StringUtils::hasText).map(String::trim).forEach(points::add);
        }
        questions.stream()
                .flatMap(question -> nullSafe(question.getKnowledgePoints()).stream())
                .filter(StringUtils::hasText)
                .map(String::trim)
                .forEach(points::add);
        List<String> distinctPoints = points.stream().distinct().toList();
        resource.setKnowledgePoints(JsonUtils.toJson(distinctPoints));
        resource.setContentMarkdown(quizResourceMarkdown(quiz, questions, distinctPoints));
        resource.setContentJson(JsonUtils.toJson(Map.of(
                "quizId", quiz.getId(),
                "spaceId", quiz.getSpaceId(),
                "questionCount", questions.size(),
                "launchPath", "/quiz?spaceId=" + quiz.getSpaceId() + "&quizId=" + quiz.getId()
        )));
        resource.setOutputSummary("包含 " + questions.size() + " 道四选一题，可在学习测验中作答并查看逐项解析。");
        resource.setQualityScore(BigDecimal.valueOf(90));
        resource.setExportStatus("interactive");
        resource.setStatus("active");
        generatedResourceMapper.insert(resource);
        return resource;
    }

    private String quizResourceMarkdown(Quiz quiz, List<AiQuizQuestionDTO> questions, List<String> points) {
        StringBuilder markdown = new StringBuilder()
                .append("# ").append(quiz.getTitle()).append("\n\n")
                .append("> 本测验已保存在当前学习空间。请在“学习测验”中作答，提交后可查看每个选项的解释。\n\n")
                .append("- 学科：").append(quiz.getSubject()).append("\n")
                .append("- 难度：").append(quiz.getDifficulty()).append("\n")
                .append("- 题目数：").append(questions.size()).append("\n")
                .append("- 知识点：").append(points.isEmpty() ? "综合复习" : String.join("、", points)).append("\n\n")
                .append("## 题目预览\n\n");
        for (AiQuizQuestionDTO question : questions) {
            markdown.append(question.getQuestionOrder()).append(". ").append(question.getStem()).append("\n\n");
            for (String option : nullSafe(question.getOptions())) {
                markdown.append("   - ").append(option).append("\n");
            }
            markdown.append("\n");
        }
        return markdown.toString();
    }
}
