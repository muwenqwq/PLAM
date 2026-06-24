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
import com.edustudio.module.learningspace.service.LearningSpaceService;
import com.edustudio.module.modelprovider.service.ModelProviderService;
import com.edustudio.module.knowledge.service.KnowledgeService;
import com.edustudio.module.knowledge.vo.KnowledgeSearchResultVO;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResourceGenerateResultVO generate(ResourceGenerateRequest request) {
        Long userId = LoginUserHolder.requireCurrentUserId();
        learningSpaceService.assertOwned(request.getSpaceId());
        Long providerId = modelProviderService.resolveProviderId(request.getModelProviderId());
        AiResourceGenerateResponse response = aiServiceClient.generateResource(AiResourceGenerateRequest.builder()
                .modelConfig(modelProviderService.resolveConfig(providerId))
                .title(request.getTitle())
                .subject(request.getSubject())
                .resourceType(request.getResourceType())
                .inputParams(JsonUtils.fromJson(JsonUtils.toJson(inputParams(request)), com.fasterxml.jackson.databind.JsonNode.class))
                .build());
        if (response == null || CollectionUtils.isEmpty(response.getResources())) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "AI 服务未返回可保存的资源内容");
        }
        GeneratedResource resource = toEntity(response.getResources().get(0), userId, request.getSpaceId(), null);
        generatedResourceMapper.insert(resource);
        knowledgeService.syncGeneratedResource(resource);
        learningSpaceService.incrementResourceCount(request.getSpaceId());
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

    private Map<String, Object> inputParams(ResourceGenerateRequest request) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("knowledge_points", request.getKnowledgePoints());
        params.put("difficulty", request.getDifficulty());
        params.put("output_length", request.getOutputLength());
        params.put("use_knowledge_base", request.getUseKnowledgeBase());
        params.put("source_file_ids", request.getSourceFileIds());
        params.put("source_file_names", request.getSourceFileNames());
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

    private <T> List<T> nullSafe(List<T> values) {
        return values == null ? List.of() : values;
    }
}
