package com.edustudio.module.resource.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edustudio.common.api.PageResult;
import com.edustudio.common.api.ResultCode;
import com.edustudio.common.exception.BusinessException;
import com.edustudio.common.security.LoginUserHolder;
import com.edustudio.common.utils.JsonUtils;
import com.edustudio.integration.ai.AiServiceClient;
import com.edustudio.integration.ai.dto.AiResourceDTO;
import com.edustudio.integration.ai.dto.AiResourceGenerateRequest;
import com.edustudio.integration.ai.dto.AiResourceGenerateResponse;
import com.edustudio.module.companion.entity.CompanionRole;
import com.edustudio.module.companion.service.CompanionRoleService;
import com.edustudio.module.companion.support.CompanionRolePayload;
import com.edustudio.module.knowledge.service.KnowledgeService;
import com.edustudio.module.knowledge.vo.KnowledgeSearchResultVO;
import com.edustudio.module.learningspace.service.LearningSpaceService;
import com.edustudio.module.modelprovider.service.ModelProviderService;
import com.edustudio.module.profile.service.UserProfileService;
import com.edustudio.module.quiz.entity.Quiz;
import com.edustudio.module.quiz.entity.QuizAnswer;
import com.edustudio.module.quiz.entity.QuizQuestion;
import com.edustudio.module.quiz.mapper.QuizAnswerMapper;
import com.edustudio.module.quiz.mapper.QuizMapper;
import com.edustudio.module.quiz.mapper.QuizQuestionMapper;
import com.edustudio.module.quiz.service.QuizService;
import com.edustudio.module.resource.dto.ResourceGenerateRequest;
import com.edustudio.module.resource.dto.ResourceQueryRequest;
import com.edustudio.module.resource.dto.ResourceUpdateRequest;
import com.edustudio.module.resource.entity.GeneratedResource;
import com.edustudio.module.resource.mapper.GeneratedResourceMapper;
import com.edustudio.module.resource.service.GeneratedResourceService;
import com.edustudio.module.resource.service.ResourceService;
import com.edustudio.module.resource.vo.GeneratedResourceVO;
import com.edustudio.module.resource.vo.ResourceGenerateResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {

    private static final String ACTIVE_STATUS = "active";

    private final GeneratedResourceMapper generatedResourceMapper;
    private final GeneratedResourceService generatedResourceService;
    private final LearningSpaceService learningSpaceService;
    private final ModelProviderService modelProviderService;
    private final AiServiceClient aiServiceClient;
    private final KnowledgeService knowledgeService;
    private final CompanionRoleService companionRoleService;
    private final UserProfileService userProfileService;
    private final QuizMapper quizMapper;
    private final QuizQuestionMapper quizQuestionMapper;
    private final QuizAnswerMapper quizAnswerMapper;
    private final QuizService quizService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResourceGenerateResultVO generate(ResourceGenerateRequest request) {
        Long userId = LoginUserHolder.requireCurrentUserId();
        learningSpaceService.assertOwned(request.getSpaceId());
        Long providerId = modelProviderService.resolveProviderId(request.getModelProviderId());
        CompanionRole role = resolveRole(request.getRolePlayEnabled(), request.getCompanionRoleId());
        Map<String, Object> rolePayload = CompanionRolePayload.from(role);
        Map<String, Object> learnerProfile = userProfileService.getAiProfile(request.getSpaceId());
        AiResourceGenerateResponse response = aiServiceClient.generateResource(AiResourceGenerateRequest.builder()
                .modelConfig(modelProviderService.resolveConfig(providerId))
                .title(request.getTitle())
                .subject(request.getSubject())
                .resourceType(request.getResourceType())
                .inputParams(JsonUtils.fromJson(JsonUtils.toJson(inputParams(request, rolePayload, learnerProfile)), com.fasterxml.jackson.databind.JsonNode.class))
                .profile(learnerProfile)
                .rolePlayEnabled(role != null)
                .companionRole(rolePayload)
                .build());
        if (response == null || CollectionUtils.isEmpty(response.getResources())) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "AI 服务未返回可保存的资源内容");
        }
        GeneratedResource resource = toEntity(response.getResources().get(0), userId, request.getSpaceId(), null);
        generatedResourceMapper.insert(resource);
        knowledgeService.syncGeneratedResource(resource);
        learningSpaceService.incrementResourceCount(request.getSpaceId());
        userProfileService.recordActivity(
                request.getSpaceId(), request.getSubject(), jsonValues(request.getKnowledgePoints()), List.of(), "resource_generation");
        return ResourceGenerateResultVO.builder()
                .resource(generatedResourceService.toVO(resource))
                .message("资源已生成并保存")
                .build();
    }

    @Override
    public PageResult<GeneratedResourceVO> page(ResourceQueryRequest request) {
        return generatedResourceService.page(request);
    }

    @Override
    public GeneratedResourceVO detail(Long id) {
        return generatedResourceService.detail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GeneratedResourceVO update(Long id, ResourceUpdateRequest request) {
        GeneratedResource resource = getOwned(id);
        resource.setTitle(request.getTitle());
        resource.setResourceType(request.getResourceType() == null ? resource.getResourceType() : request.getResourceType());
        resource.setSubject(request.getSubject() == null ? resource.getSubject() : request.getSubject());
        resource.setContentMarkdown(request.getContentMarkdown());
        resource.setContentJson(request.getContentJson() == null ? resource.getContentJson() : JsonUtils.toJson(request.getContentJson()));
        resource.setOutputSummary(request.getOutputSummary());
        resource.setStatus(request.getStatus() == null ? resource.getStatus() : request.getStatus());
        generatedResourceMapper.updateById(resource);
        knowledgeService.syncGeneratedResource(resource);
        return generatedResourceService.toVO(resource);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        GeneratedResource resource = getOwned(id);
        if ("quiz_set".equals(resource.getResourceType()) && quizService.deleteByResourceId(resource.getId())) {
            return;
        }
        generatedResourceMapper.deleteById(id);
        knowledgeService.deleteGeneratedResourceIndex(resource.getId(), resource.getUserId());
    }

    @Override
    public String exportMarkdown(Long id) {
        GeneratedResource resource = getOwned(id);
        return resource.getContentMarkdown() == null ? "" : resource.getContentMarkdown();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GeneratedResourceVO generateGraph(Long id) {
        GeneratedResource source = getOwned(id);
        ResourceGenerateRequest request = new ResourceGenerateRequest();
        request.setSpaceId(source.getSpaceId());
        request.setTitle(source.getTitle() + " 知识图谱");
        request.setSubject(source.getSubject());
        request.setResourceType("knowledge_graph");
        request.setKnowledgePoints(JsonUtils.fromJson(source.getKnowledgePoints(), com.fasterxml.jackson.databind.JsonNode.class));
        return generate(request).getResource();
    }

    private GeneratedResource getOwned(Long id) {
        GeneratedResource resource = generatedResourceMapper.selectOne(new LambdaQueryWrapper<GeneratedResource>()
                .eq(GeneratedResource::getId, id)
                .eq(GeneratedResource::getUserId, LoginUserHolder.requireCurrentUserId())
                .eq(GeneratedResource::getDeleted, 0));
        if (resource == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "资源不存在或无权访问");
        }
        return resource;
    }

    private GeneratedResource toEntity(AiResourceDTO dto, Long userId, Long spaceId, Long taskId) {
        GeneratedResource resource = new GeneratedResource();
        resource.setUserId(userId);
        resource.setSpaceId(spaceId);
        resource.setTaskId(taskId);
        resource.setResourceType(dto.getResourceType());
        resource.setTitle(dto.getTitle());
        resource.setSubject(dto.getSubject());
        resource.setKnowledgePoints(dto.getKnowledgePoints() == null ? "[]" : JsonUtils.toJson(dto.getKnowledgePoints()));
        resource.setContentMarkdown(dto.getContentMarkdown());
        resource.setContentJson(dto.getContentJson() == null ? "{}" : JsonUtils.toJson(dto.getContentJson()));
        resource.setOutputSummary(dto.getOutputSummary());
        resource.setQualityScore(dto.getQualityScore());
        resource.setExportStatus("markdown");
        resource.setStatus(ACTIVE_STATUS);
        return resource;
    }

    private Map<String, Object> inputParams(ResourceGenerateRequest request, Map<String, Object> rolePayload,
                                            Map<String, Object> learnerProfile) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("knowledge_points", request.getKnowledgePoints());
        params.put("difficulty", request.getDifficulty());
        params.put("output_length", request.getOutputLength());
        params.put("use_knowledge_base", request.getUseKnowledgeBase());
        params.put("source_file_ids", request.getSourceFileIds());
        params.put("source_file_names", request.getSourceFileNames());
        params.put("learner_profile", learnerProfile);
        if ("mistake_review".equals(request.getResourceType())) {
            params.put("mistakes", loadMistakes(request.getSpaceId()));
        }
        if (!rolePayload.isEmpty()) {
            params.put("role_play_enabled", true);
            params.put("companion_role", rolePayload);
        }
        if (Boolean.TRUE.equals(request.getUseKnowledgeBase()) || !CollectionUtils.isEmpty(request.getSourceFileIds())) {
            KnowledgeSearchResultVO context = knowledgeService.generationContext(
                    request.getSpaceId(), request.getSourceFileIds(), 8);
            List<Map<String, Object>> chunks = new ArrayList<>();
            for (KnowledgeSearchResultVO.Item item : nullSafe(context.getResults())) {
                Map<String, Object> chunk = new LinkedHashMap<>();
                chunk.put("source", item.getSource());
                chunk.put("file_id", item.getFileId());
                chunk.put("chunk_index", item.getChunkIndex());
                chunk.put("content", item.getChunkText());
                chunks.add(chunk);
            }
            if (!CollectionUtils.isEmpty(request.getSourceFileIds()) && chunks.isEmpty()) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "所选资料尚未生成可用知识片段，请等待处理完成后再生成资源");
            }
            params.put("knowledge_context", chunks);
            params.put("grounding_required", !chunks.isEmpty());
        }
        return params;
    }

    private CompanionRole resolveRole(Boolean enabled, Long roleId) {
        if (!Boolean.TRUE.equals(enabled)) {
            return null;
        }
        return companionRoleService.getOwnedActiveRole(roleId);
    }

    private <T> List<T> nullSafe(List<T> values) {
        return values == null ? List.of() : values;
    }

    private List<String> jsonValues(com.fasterxml.jackson.databind.JsonNode value) {
        if (value == null || !value.isArray()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        value.forEach(item -> {
            if (item.isTextual() && !item.asText().isBlank()) {
                result.add(item.asText());
            }
        });
        return result;
    }

    private List<Map<String, Object>> loadMistakes(Long spaceId) {
        Long userId = LoginUserHolder.requireCurrentUserId();
        List<Quiz> quizzes = quizMapper.selectList(new LambdaQueryWrapper<Quiz>()
                .eq(Quiz::getUserId, userId)
                .eq(Quiz::getSpaceId, spaceId)
                .eq(Quiz::getStatus, "submitted")
                .eq(Quiz::getDeleted, 0)
                .orderByDesc(Quiz::getCreatedAt));
        if (quizzes.isEmpty()) {
            return List.of();
        }
        List<Long> quizIds = quizzes.stream().map(Quiz::getId).toList();
        Map<Long, Quiz> quizMap = quizzes.stream().collect(java.util.stream.Collectors.toMap(Quiz::getId, item -> item));
        List<QuizAnswer> wrongAnswers = quizAnswerMapper.selectList(new LambdaQueryWrapper<QuizAnswer>()
                .eq(QuizAnswer::getUserId, userId)
                .in(QuizAnswer::getQuizId, quizIds)
                .eq(QuizAnswer::getIsCorrect, false)
                .eq(QuizAnswer::getDeleted, 0));
        if (wrongAnswers.isEmpty()) {
            return List.of();
        }
        Map<Long, QuizAnswer> answerByQuestion = new HashMap<>();
        for (QuizAnswer answer : wrongAnswers) {
            answerByQuestion.put(answer.getQuestionId(), answer);
        }
        List<QuizQuestion> questions = quizQuestionMapper.selectList(new LambdaQueryWrapper<QuizQuestion>()
                .eq(QuizQuestion::getUserId, userId)
                .in(QuizQuestion::getId, answerByQuestion.keySet())
                .eq(QuizQuestion::getDeleted, 0));
        questions.sort(Comparator
                .comparing((QuizQuestion item) -> quizMap.get(item.getQuizId()).getCreatedAt(), Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(QuizQuestion::getQuestionOrder, Comparator.nullsLast(Comparator.naturalOrder())));
        List<Map<String, Object>> result = new ArrayList<>();
        for (QuizQuestion question : questions.stream().limit(30).toList()) {
            Quiz quiz = quizMap.get(question.getQuizId());
            QuizAnswer answer = answerByQuestion.get(question.getId());
            if (quiz == null || answer == null) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("quiz_id", quiz.getId());
            item.put("quiz_title", quiz.getTitle());
            item.put("question_id", question.getId());
            item.put("question_order", question.getQuestionOrder());
            item.put("stem", question.getStem());
            item.put("options", parseJsonValue(question.getOptionsJson(), List.class, List.of()));
            item.put("student_answer", answer.getAnswerText());
            item.put("correct_answer", question.getAnswerText());
            item.put("analysis", question.getAnalysisText());
            item.put("option_explanations", parseJsonValue(question.getOptionAnalysisJson(), Map.class, Map.of()));
            item.put("knowledge_points", parseJsonValue(question.getKnowledgePoints(), List.class, List.of()));
            result.add(item);
        }
        return result;
    }

    private Object parseJsonValue(String json, Class<?> type, Object fallback) {
        if (json == null || json.isBlank()) {
            return fallback;
        }
        try {
            return JsonUtils.fromJson(json, type);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
