package com.edustudio.module.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edustudio.common.api.PageResult;
import com.edustudio.common.api.ResultCode;
import com.edustudio.common.exception.BusinessException;
import com.edustudio.common.security.LoginUserHolder;
import com.edustudio.common.utils.JsonUtils;
import com.edustudio.integration.ai.AiServiceClient;
import com.edustudio.integration.ai.dto.AiRagChunkDTO;
import com.edustudio.integration.ai.dto.AiRagIndexRequest;
import com.edustudio.integration.ai.dto.AiRagIndexResponse;
import com.edustudio.integration.ai.dto.AiRagQaRequest;
import com.edustudio.integration.ai.dto.AiRagQaResponse;
import com.edustudio.integration.ai.dto.AiRagSearchRequest;
import com.edustudio.integration.ai.dto.AiRagSearchResponse;
import com.edustudio.module.knowledge.dto.KnowledgeFileCreateRequest;
import com.edustudio.module.knowledge.dto.KnowledgeFileQueryRequest;
import com.edustudio.module.knowledge.dto.KnowledgeIndexRequest;
import com.edustudio.module.knowledge.dto.KnowledgeSearchRequest;
import com.edustudio.module.knowledge.entity.KnowledgeChunk;
import com.edustudio.module.knowledge.entity.KnowledgeFile;
import com.edustudio.module.knowledge.mapper.KnowledgeChunkMapper;
import com.edustudio.module.knowledge.mapper.KnowledgeFileMapper;
import com.edustudio.module.knowledge.service.KnowledgeService;
import com.edustudio.module.knowledge.vo.KnowledgeFileVO;
import com.edustudio.module.knowledge.vo.KnowledgeSearchResultVO;
import com.edustudio.module.learningspace.service.LearningSpaceService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeServiceImpl implements KnowledgeService {

    private static final String ACTIVE_STATUS = "active";

    private final KnowledgeFileMapper knowledgeFileMapper;
    private final KnowledgeChunkMapper knowledgeChunkMapper;
    private final LearningSpaceService learningSpaceService;
    private final AiServiceClient aiServiceClient;

    @Override
    public KnowledgeFileVO create(KnowledgeFileCreateRequest request) {
        Long userId = LoginUserHolder.requireCurrentUserId();
        learningSpaceService.assertOwned(request.getSpaceId());
        KnowledgeFile file = new KnowledgeFile();
        file.setUserId(userId);
        file.setSpaceId(request.getSpaceId());
        file.setOriginalName(request.getOriginalName());
        file.setStoragePath(request.getStoragePath());
        file.setFileType(request.getFileType());
        file.setFileSize(request.getFileSize());
        file.setParserStatus("pending");
        file.setChunkCount(0);
        file.setStatus(ACTIVE_STATUS);
        knowledgeFileMapper.insert(file);
        return toVO(file);
    }

    @Override
    public PageResult<KnowledgeFileVO> page(KnowledgeFileQueryRequest request) {
        Long userId = LoginUserHolder.requireCurrentUserId();
        LambdaQueryWrapper<KnowledgeFile> wrapper = new LambdaQueryWrapper<KnowledgeFile>()
                .eq(KnowledgeFile::getUserId, userId)
                .eq(KnowledgeFile::getDeleted, 0)
                .eq(request.getSpaceId() != null, KnowledgeFile::getSpaceId, request.getSpaceId())
                .eq(StringUtils.hasText(request.getParserStatus()), KnowledgeFile::getParserStatus, request.getParserStatus())
                .like(StringUtils.hasText(request.getKeyword()), KnowledgeFile::getOriginalName, request.getKeyword())
                .orderByDesc(KnowledgeFile::getCreatedAt);
        Page<KnowledgeFile> result = knowledgeFileMapper.selectPage(new Page<>(request.getPageNum(), request.getPageSize()), wrapper);
        return PageResult.of(result.getRecords().stream().map(this::toVO).toList(), result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public KnowledgeFileVO detail(Long id) {
        return toVO(getOwnedFile(id));
    }

    @Override
    public void delete(Long id) {
        KnowledgeFile file = getOwnedFile(id);
        knowledgeFileMapper.deleteById(file);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeFileVO index(Long id, KnowledgeIndexRequest request) {
        KnowledgeFile file = getOwnedFile(id);
        AiRagIndexResponse response = aiServiceClient.indexKnowledge(AiRagIndexRequest.builder()
                .fileId(file.getId())
                .fileName(file.getOriginalName())
                .fileType(file.getFileType())
                .sourceText(request.getSourceText())
                .metadata(new HashMap<>())
                .build());
        knowledgeChunkMapper.delete(new LambdaQueryWrapper<KnowledgeChunk>()
                .eq(KnowledgeChunk::getKnowledgeFileId, file.getId())
                .eq(KnowledgeChunk::getUserId, file.getUserId()));
        for (AiRagChunkDTO chunkDTO : nullSafe(response.getChunks())) {
            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setKnowledgeFileId(file.getId());
            chunk.setUserId(file.getUserId());
            chunk.setSpaceId(file.getSpaceId());
            chunk.setChunkIndex(chunkDTO.getChunkIndex());
            chunk.setContentText(chunkDTO.getContentText());
            chunk.setContentHash(chunkDTO.getContentHash());
            chunk.setTokenCount(chunkDTO.getTokenCount());
            chunk.setMetadata(chunkDTO.getMetadata() == null ? "{}" : JsonUtils.toJson(chunkDTO.getMetadata()));
            chunk.setEmbeddingRef(chunkDTO.getEmbeddingRef());
            chunk.setStatus(ACTIVE_STATUS);
            knowledgeChunkMapper.insert(chunk);
        }
        file.setParserStatus(StringUtils.hasText(response.getParserStatus()) ? response.getParserStatus() : "indexed");
        file.setChunkCount(response.getChunkCount() == null ? 0 : response.getChunkCount());
        file.setErrorMessage(null);
        knowledgeFileMapper.updateById(file);
        return toVO(file);
    }

    @Override
    public KnowledgeSearchResultVO search(KnowledgeSearchRequest request) {
        assertFilesOwned(request);
        AiRagSearchResponse response = aiServiceClient.searchKnowledge(AiRagSearchRequest.builder()
                .query(request.getQuery())
                .fileIds(request.getFileIds())
                .topK(request.getTopK())
                .filters(Collections.emptyMap())
                .build());
        return KnowledgeSearchResultVO.builder()
                .query(request.getQuery())
                .results(toItems(response.getResults()))
                .build();
    }

    @Override
    public KnowledgeSearchResultVO qa(KnowledgeSearchRequest request) {
        assertFilesOwned(request);
        AiRagQaResponse response = aiServiceClient.answerKnowledge(AiRagQaRequest.builder()
                .query(request.getQuery())
                .fileIds(request.getFileIds())
                .topK(request.getTopK())
                .build());
        return KnowledgeSearchResultVO.builder()
                .query(request.getQuery())
                .answerMarkdown(response.getAnswerMarkdown())
                .results(toItems(response.getCitations()))
                .build();
    }

    private void assertFilesOwned(KnowledgeSearchRequest request) {
        Long userId = LoginUserHolder.requireCurrentUserId();
        if (request.getSpaceId() != null) {
            learningSpaceService.assertOwned(request.getSpaceId());
        }
        for (Long fileId : request.getFileIds() == null ? Collections.<Long>emptyList() : request.getFileIds()) {
            KnowledgeFile file = knowledgeFileMapper.selectOne(new LambdaQueryWrapper<KnowledgeFile>()
                    .eq(KnowledgeFile::getId, fileId)
                    .eq(KnowledgeFile::getUserId, userId)
                    .eq(KnowledgeFile::getDeleted, 0));
            if (file == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "知识库文件不存在或无权访问");
            }
        }
    }

    private KnowledgeFile getOwnedFile(Long id) {
        KnowledgeFile file = knowledgeFileMapper.selectOne(new LambdaQueryWrapper<KnowledgeFile>()
                .eq(KnowledgeFile::getId, id)
                .eq(KnowledgeFile::getUserId, LoginUserHolder.requireCurrentUserId())
                .eq(KnowledgeFile::getDeleted, 0));
        if (file == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "知识库文件不存在或无权访问");
        }
        return file;
    }

    private KnowledgeFileVO toVO(KnowledgeFile file) {
        return KnowledgeFileVO.builder()
                .id(file.getId())
                .userId(file.getUserId())
                .spaceId(file.getSpaceId())
                .originalName(file.getOriginalName())
                .storagePath(file.getStoragePath())
                .fileType(file.getFileType())
                .fileSize(file.getFileSize())
                .parserStatus(file.getParserStatus())
                .chunkCount(file.getChunkCount())
                .errorMessage(file.getErrorMessage())
                .status(file.getStatus())
                .createdAt(file.getCreatedAt())
                .updatedAt(file.getUpdatedAt())
                .build();
    }

    private List<KnowledgeSearchResultVO.Item> toItems(List<AiRagChunkDTO> chunks) {
        return nullSafe(chunks).stream().map(chunk -> KnowledgeSearchResultVO.Item.builder()
                .source(chunk.getSource())
                .chunkIndex(chunk.getChunkIndex())
                .chunkText(chunk.getContentText())
                .score(chunk.getScore())
                .metadata(chunk.getMetadata() == null ? null : JsonUtils.fromJson(JsonUtils.toJson(chunk.getMetadata()), JsonNode.class))
                .build()).toList();
    }

    private <T> List<T> nullSafe(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }
}
