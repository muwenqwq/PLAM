package com.edustudio.module.resource.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.edustudio.common.api.PageResult;
import com.edustudio.common.api.ResultCode;
import com.edustudio.common.exception.BusinessException;
import com.edustudio.common.security.LoginUserHolder;
import com.edustudio.common.utils.JsonUtils;
import com.edustudio.module.resource.dto.GeneratedResourceQueryRequest;
import com.edustudio.module.resource.entity.GeneratedResource;
import com.edustudio.module.resource.mapper.GeneratedResourceMapper;
import com.edustudio.module.resource.service.GeneratedResourceService;
import com.edustudio.module.resource.vo.GeneratedResourceVO;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class GeneratedResourceServiceImpl extends ServiceImpl<GeneratedResourceMapper, GeneratedResource>
        implements GeneratedResourceService {

    @Override
    public PageResult<GeneratedResourceVO> page(GeneratedResourceQueryRequest request) {
        Long userId = LoginUserHolder.requireCurrentUserId();
        LambdaQueryWrapper<GeneratedResource> wrapper = new LambdaQueryWrapper<GeneratedResource>()
                .eq(GeneratedResource::getUserId, userId)
                .eq(GeneratedResource::getDeleted, 0)
                .eq(request.getSpaceId() != null, GeneratedResource::getSpaceId, request.getSpaceId())
                .eq(request.getTaskId() != null, GeneratedResource::getTaskId, request.getTaskId())
                .eq(StringUtils.hasText(request.getResourceType()), GeneratedResource::getResourceType, request.getResourceType())
                .like(StringUtils.hasText(request.getKeyword()), GeneratedResource::getTitle, request.getKeyword())
                .orderByDesc(GeneratedResource::getCreatedAt);
        Page<GeneratedResource> result = page(new Page<>(request.getPageNum(), request.getPageSize()), wrapper);
        List<GeneratedResourceVO> records = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public GeneratedResourceVO detail(Long id) {
        GeneratedResource entity = getOne(new LambdaQueryWrapper<GeneratedResource>()
                .eq(GeneratedResource::getId, id)
                .eq(GeneratedResource::getUserId, LoginUserHolder.requireCurrentUserId())
                .eq(GeneratedResource::getDeleted, 0), false);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "生成资源不存在或无权访问");
        }
        return toVO(entity);
    }

    @Override
    public GeneratedResourceVO toVO(GeneratedResource entity) {
        return GeneratedResourceVO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .spaceId(entity.getSpaceId())
                .taskId(entity.getTaskId())
                .resourceType(entity.getResourceType())
                .title(entity.getTitle())
                .subject(entity.getSubject())
                .knowledgePoints(parseJson(entity.getKnowledgePoints()))
                .contentMarkdown(entity.getContentMarkdown())
                .contentJson(parseJson(entity.getContentJson()))
                .outputSummary(entity.getOutputSummary())
                .qualityScore(entity.getQualityScore())
                .exportStatus(entity.getExportStatus())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private JsonNode parseJson(String json) {
        return StringUtils.hasText(json) ? JsonUtils.fromJson(json, JsonNode.class) : null;
    }
}
