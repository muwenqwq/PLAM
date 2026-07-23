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
import com.edustudio.module.knowledge.dto.KnowledgeFileCreateRequest;
import com.edustudio.module.knowledge.dto.KnowledgeFileQueryRequest;
import com.edustudio.module.knowledge.dto.KnowledgeIndexRequest;
import com.edustudio.module.knowledge.dto.KnowledgeSearchRequest;
import com.edustudio.module.knowledge.entity.KnowledgeChunk;
import com.edustudio.module.knowledge.entity.KnowledgeFile;
import com.edustudio.module.knowledge.mapper.KnowledgeChunkMapper;
import com.edustudio.module.knowledge.mapper.KnowledgeFileMapper;
import com.edustudio.module.knowledge.service.KnowledgeService;
import com.edustudio.module.knowledge.vo.KnowledgeChunkVO;
import com.edustudio.module.knowledge.vo.KnowledgeFileVO;
import com.edustudio.module.knowledge.vo.KnowledgeSearchResultVO;
import com.edustudio.module.learningspace.service.LearningSpaceService;
import com.edustudio.module.learningspace.support.KnowledgeStorageCleaner;
import com.edustudio.module.modelprovider.service.ModelProviderService;
import com.edustudio.module.profile.service.UserProfileService;
import com.edustudio.module.resource.entity.GeneratedResource;
import com.edustudio.module.resource.mapper.GeneratedResourceMapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeServiceImpl implements KnowledgeService {

    private static final String ACTIVE_STATUS = "active";

    private final KnowledgeFileMapper knowledgeFileMapper;
    private final KnowledgeChunkMapper knowledgeChunkMapper;
    private final LearningSpaceService learningSpaceService;
    private final AiServiceClient aiServiceClient;
    private final ModelProviderService modelProviderService;
    private final KnowledgeFileTextExtractor textExtractor;
    private final GeneratedResourceMapper generatedResourceMapper;
    private final UserProfileService userProfileService;
    private final KnowledgeStorageCleaner storageCleaner;

    @Value("${eduagent.knowledge.storage-dir:${user.dir}/data/knowledge}")
    private String storageDir;

    @Value("${eduagent.rag.vector-backend:mysql}")
    private String ragVectorBackend;

    @Override
    public KnowledgeFileVO create(KnowledgeFileCreateRequest request) {
        Long userId = LoginUserHolder.requireCurrentUserId();
        learningSpaceService.assertOwned(request.getSpaceId());
        KnowledgeFile file = new KnowledgeFile();
        file.setUserId(userId);
        file.setSpaceId(request.getSpaceId());
        file.setOriginalName(request.getOriginalName());
        file.setStoragePath(StringUtils.hasText(request.getStoragePath())
                ? request.getStoragePath()
                : "knowledge/" + userId + "/" + request.getSpaceId() + "/metadata/" + request.getOriginalName());
        file.setFileType(request.getFileType());
        file.setFileSize(request.getFileSize());
        file.setParserStatus("pending");
        file.setChunkCount(0);
        file.setStatus(ACTIVE_STATUS);
        knowledgeFileMapper.insert(file);
        return toVO(file);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeFileVO upload(Long spaceId, MultipartFile multipartFile) {
        Long userId = LoginUserHolder.requireCurrentUserId();
        learningSpaceService.assertOwned(spaceId);
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "上传文件不能为空");
        }
        byte[] bytes = readBytes(multipartFile);
        String originalName = sanitizeOriginalName(multipartFile.getOriginalFilename());
        String fileType = detectFileType(originalName);
        String sourceText = textExtractor.extract(originalName, multipartFile.getContentType(), bytes);
        String checksum = sha256(bytes);
        String storagePath = saveFile(userId, spaceId, originalName, bytes);

        KnowledgeFile file = new KnowledgeFile();
        file.setUserId(userId);
        file.setSpaceId(spaceId);
        file.setOriginalName(originalName);
        file.setStoragePath(storagePath);
        file.setFileType(fileType);
        file.setFileSize((long) bytes.length);
        file.setChecksum(checksum);
        file.setParserStatus("pending");
        file.setChunkCount(0);
        file.setStatus(ACTIVE_STATUS);
        knowledgeFileMapper.insert(file);

        KnowledgeIndexRequest indexRequest = new KnowledgeIndexRequest();
        indexRequest.setSourceText(sourceText);
        return index(file.getId(), indexRequest);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeFileVO syncGeneratedResource(GeneratedResource resource) {
        if (resource == null || resource.getId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Generated resource is required");
        }
        if (!ACTIVE_STATUS.equals(resource.getStatus())) {
            deleteGeneratedResourceIndex(resource.getId(), resource.getUserId());
            return null;
        }
        String sourceText = buildResourceText(resource);
        if (!StringUtils.hasText(sourceText)) {
            deleteGeneratedResourceIndex(resource.getId(), resource.getUserId());
            return null;
        }
        String storagePath = generatedResourceStoragePath(resource.getId());
        KnowledgeFile file = knowledgeFileMapper.selectOne(new LambdaQueryWrapper<KnowledgeFile>()
                .eq(KnowledgeFile::getUserId, resource.getUserId())
                .eq(KnowledgeFile::getStoragePath, storagePath)
                .eq(KnowledgeFile::getDeleted, 0)
                .last("LIMIT 1"));
        if (file == null) {
            file = new KnowledgeFile();
            file.setUserId(resource.getUserId());
            file.setStoragePath(storagePath);
            file.setParserStatus("pending");
            file.setChunkCount(0);
            file.setStatus(ACTIVE_STATUS);
        }
        byte[] bytes = sourceText.getBytes(StandardCharsets.UTF_8);
        file.setSpaceId(resource.getSpaceId());
        file.setOriginalName(generatedResourceName(resource));
        file.setFileType("generated_resource");
        file.setFileSize((long) bytes.length);
        file.setChecksum(sha256(bytes));
        file.setErrorMessage(null);
        if (file.getId() == null) {
            knowledgeFileMapper.insert(file);
        } else {
            knowledgeFileMapper.updateById(file);
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source_type", "generated_resource");
        metadata.put("resource_id", resource.getId());
        metadata.put("resource_type", resource.getResourceType());
        metadata.put("resource_title", resource.getTitle());
        return indexFile(file, sourceText, metadata);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGeneratedResourceIndex(Long resourceId, Long userId) {
        if (resourceId == null || userId == null) {
            return;
        }
        KnowledgeFile file = knowledgeFileMapper.selectOne(new LambdaQueryWrapper<KnowledgeFile>()
                .eq(KnowledgeFile::getUserId, userId)
                .eq(KnowledgeFile::getStoragePath, generatedResourceStoragePath(resourceId))
                .eq(KnowledgeFile::getDeleted, 0)
                .last("LIMIT 1"));
        if (file == null) {
            return;
        }
        deleteVectorIndex(file.getId());
        knowledgeChunkMapper.delete(new LambdaQueryWrapper<KnowledgeChunk>()
                .eq(KnowledgeChunk::getKnowledgeFileId, file.getId())
                .eq(KnowledgeChunk::getUserId, userId));
        knowledgeFileMapper.deleteById(file);
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
        KnowledgeFile file = getOwnedFile(id);
        List<KnowledgeChunk> chunkList = knowledgeChunkMapper.selectList(new LambdaQueryWrapper<KnowledgeChunk>()
                .eq(KnowledgeChunk::getKnowledgeFileId, file.getId())
                .eq(KnowledgeChunk::getDeleted, 0)
                .orderByAsc(KnowledgeChunk::getChunkIndex));
        KnowledgeFileVO vo = toVO(file);
        List<KnowledgeChunk> readableChunks = chunkList.stream()
                .filter(chunk -> !looksLikeRawOfficeZip(chunk.getContentText()))
                .toList();
        vo.setChunks(readableChunks.stream().map(this::toChunkVO).toList());
        if (readableChunks.size() < chunkList.size() && !StringUtils.hasText(vo.getErrorMessage())) {
            vo.setErrorMessage("该资料存在旧版乱码片段，请点击重新整理资料。");
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        KnowledgeFile file = getOwnedFile(id);
        deleteVectorIndex(file.getId());
        knowledgeChunkMapper.delete(new LambdaQueryWrapper<KnowledgeChunk>()
                .eq(KnowledgeChunk::getKnowledgeFileId, file.getId())
                .eq(KnowledgeChunk::getUserId, file.getUserId()));
        knowledgeFileMapper.deleteById(file);
        storageCleaner.deleteStoredFile(file);
    }

    private void deleteVectorIndex(Long fileId) {
        try {
            aiServiceClient.deleteKnowledgeIndex(fileId);
        } catch (BusinessException exception) {
            log.warn("Failed to delete vector index for knowledge file {}: {}", fileId, exception.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeFileVO index(Long id, KnowledgeIndexRequest request) {
        KnowledgeFile file = getOwnedFile(id);
        String sourceText = resolveSourceText(file, request);
        return indexFile(file, sourceText, Map.of("source_type", "uploaded_file"));
    }

    private KnowledgeFileVO indexFile(KnowledgeFile file, String sourceText, Map<String, Object> extraMetadata) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("user_id", file.getUserId());
        metadata.put("space_id", file.getSpaceId());
        metadata.put("storage_path", file.getStoragePath());
        metadata.put("checksum", valueOrBlank(file.getChecksum()));
        if (extraMetadata != null) {
            metadata.putAll(extraMetadata);
        }
        AiRagIndexResponse response = aiServiceClient.indexKnowledge(AiRagIndexRequest.builder()
                .modelConfig(modelProviderService.resolveConfig(null))
                .fileId(file.getId())
                .fileName(file.getOriginalName())
                .fileType(file.getFileType())
                .sourceText(sourceText)
                .metadata(metadata)
                .build());
        if (response == null || !Boolean.TRUE.equals(response.getSuccess())) {
            file.setParserStatus("failed");
            file.setErrorMessage("AI 服务未能完成知识片段生成");
            knowledgeFileMapper.updateById(file);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "AI 服务未能完成知识片段生成");
        }
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
        List<KnowledgeSearchResultVO.Item> results = findActualResults(request);
        return KnowledgeSearchResultVO.builder()
                .query(request.getQuery())
                .results(results)
                .build();
    }

    @Override
    public KnowledgeSearchResultVO generationContext(Long spaceId, List<Long> fileIds, Integer topK) {
        KnowledgeSearchRequest request = new KnowledgeSearchRequest();
        request.setSpaceId(spaceId);
        request.setFileIds(fileIds == null ? Collections.emptyList() : fileIds);
        request.setQuery("resource generation context");
        assertFilesOwned(request);

        Long userId = LoginUserHolder.requireCurrentUserId();
        int limit = topK == null ? 8 : Math.max(1, Math.min(20, topK));
        List<KnowledgeFile> files = loadSearchFiles(userId, spaceId, request.getFileIds());
        if (files.isEmpty()) {
            return KnowledgeSearchResultVO.builder().query(request.getQuery()).results(Collections.emptyList()).build();
        }
        Map<Long, KnowledgeFile> fileMap = new HashMap<>();
        for (KnowledgeFile file : files) {
            fileMap.put(file.getId(), file);
        }
        List<KnowledgeSearchResultVO.Item> results = knowledgeChunkMapper.selectList(
                        new LambdaQueryWrapper<KnowledgeChunk>()
                                .eq(KnowledgeChunk::getUserId, userId)
                                .eq(KnowledgeChunk::getDeleted, 0)
                                .eq(KnowledgeChunk::getStatus, ACTIVE_STATUS)
                                .in(KnowledgeChunk::getKnowledgeFileId, fileMap.keySet())
                                .orderByAsc(KnowledgeChunk::getKnowledgeFileId)
                                .orderByAsc(KnowledgeChunk::getChunkIndex))
                .stream()
                .filter(chunk -> StringUtils.hasText(chunk.getContentText()) && !looksLikeRawOfficeZip(chunk.getContentText()))
                .limit(limit)
                .map(chunk -> {
                    KnowledgeFile file = fileMap.get(chunk.getKnowledgeFileId());
                    Map<String, Object> metadata = new LinkedHashMap<>();
                    metadata.put("source_type", file != null && "generated_resource".equals(file.getFileType())
                            ? "generated_resource" : "knowledge_file");
                    metadata.put("file_id", chunk.getKnowledgeFileId());
                    metadata.put("space_id", chunk.getSpaceId());
                    metadata.put("retrieval_mode", retrievalMode());
                    return KnowledgeSearchResultVO.Item.builder()
                            .fileId(chunk.getKnowledgeFileId())
                            .source(file == null ? "学习资料" : file.getOriginalName())
                            .sourceFileName(file == null ? "学习资料" : file.getOriginalName())
                            .chunkIndex(chunk.getChunkIndex())
                            .chunkText(chunk.getContentText())
                            .score(BigDecimal.ONE)
                            .retrievalMode(retrievalMode())
                            .metadata(JsonUtils.fromJson(JsonUtils.toJson(metadata), JsonNode.class))
                            .build();
                })
                .toList();
        return KnowledgeSearchResultVO.builder().query(request.getQuery()).results(results).build();
    }
    @Override
    public KnowledgeSearchResultVO qa(KnowledgeSearchRequest request) {
        assertFilesOwned(request);
        List<KnowledgeSearchResultVO.Item> results = findActualResults(request);
        String answer = buildGroundedAnswer(request.getQuery(), results);
        if (!results.isEmpty()) {
            try {
                Long providerId = modelProviderService.resolveProviderId(request.getModelProviderId());
                AiRagQaResponse response = aiServiceClient.answerKnowledge(AiRagQaRequest.builder()
                        .modelConfig(modelProviderService.resolveConfig(providerId))
                        .query(request.getQuery())
                        .fileIds(request.getFileIds())
                        .topK(request.getTopK())
                        .context(results.stream().map(this::toAiRagChunk).toList())
                        .profile(userProfileService.getAiProfile(request.getSpaceId()))
                        .history(request.getHistory() == null ? List.of() : request.getHistory().stream()
                                .map(item -> Map.of("role", item.getRole(), "content", item.getContent()))
                                .toList())
                        .build());
                if (response != null && StringUtils.hasText(response.getAnswerMarkdown())) {
                    answer = response.getAnswerMarkdown();
                }
            } catch (BusinessException ignored) {
                // Keep the grounded local answer when the AI service is temporarily unavailable.
            }
        }
        userProfileService.recordActivity(request.getSpaceId(), null, List.of(), List.of(), "knowledge_qa");
        return KnowledgeSearchResultVO.builder()
                .query(request.getQuery())
                .answerMarkdown(answer)
                .results(results)
                .build();
    }

    private void assertFilesOwned(KnowledgeSearchRequest request) {
        Long userId = LoginUserHolder.requireCurrentUserId();
        List<Long> fileIds = request.getFileIds() == null ? Collections.emptyList() : request.getFileIds();
        if (request.getSpaceId() == null && fileIds.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Please select a learning space before searching knowledge");
        }
        if (request.getSpaceId() != null) {
            learningSpaceService.assertOwned(request.getSpaceId());
        }
        for (Long fileId : fileIds) {
            KnowledgeFile file = knowledgeFileMapper.selectOne(new LambdaQueryWrapper<KnowledgeFile>()
                    .eq(KnowledgeFile::getId, fileId)
                    .eq(KnowledgeFile::getUserId, userId)
                    .eq(request.getSpaceId() != null, KnowledgeFile::getSpaceId, request.getSpaceId())
                    .eq(KnowledgeFile::getDeleted, 0));
            if (file == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "知识库文件不存在或无权访问");
            }
        }
    }

    private List<KnowledgeSearchResultVO.Item> findActualResults(KnowledgeSearchRequest request) {
        Long userId = LoginUserHolder.requireCurrentUserId();
        int topK = request.getTopK() == null ? 5 : Math.max(1, Math.min(20, request.getTopK()));
        List<Long> fileIds = request.getFileIds() == null ? Collections.emptyList() : request.getFileIds();
        List<KnowledgeSearchResultVO.Item> vectorResults = findVectorResults(request, userId, topK);
        if (!vectorResults.isEmpty()) {
            return vectorResults;
        }
        List<KnowledgeFile> files = loadSearchFiles(userId, request.getSpaceId(), fileIds);
        Map<Long, KnowledgeFile> fileMap = new HashMap<>();
        Set<Long> spaceIds = new HashSet<>();
        for (KnowledgeFile file : files) {
            fileMap.put(file.getId(), file);
            if (file.getSpaceId() != null) {
                spaceIds.add(file.getSpaceId());
            }
        }

        List<SearchCandidate> candidates = new ArrayList<>();
        if (!files.isEmpty()) {
            LambdaQueryWrapper<KnowledgeChunk> chunkWrapper = new LambdaQueryWrapper<KnowledgeChunk>()
                    .eq(KnowledgeChunk::getUserId, userId)
                    .eq(KnowledgeChunk::getDeleted, 0)
                    .eq(KnowledgeChunk::getStatus, ACTIVE_STATUS)
                    .in(KnowledgeChunk::getKnowledgeFileId, fileMap.keySet());
            List<KnowledgeChunk> chunks = knowledgeChunkMapper.selectList(chunkWrapper);
            for (KnowledgeChunk chunk : chunks) {
                if (!StringUtils.hasText(chunk.getContentText()) || looksLikeRawOfficeZip(chunk.getContentText())) {
                    continue;
                }
                KnowledgeFile file = fileMap.get(chunk.getKnowledgeFileId());
                String source = file == null ? "uploaded file" : file.getOriginalName();
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("source_type", "knowledge_file");
                metadata.put("file_id", chunk.getKnowledgeFileId());
                metadata.put("space_id", chunk.getSpaceId());
                metadata.put("retrieval_mode", retrievalMode());
                candidates.add(new SearchCandidate(source, chunk.getChunkIndex(), chunk.getContentText(), metadata));
            }
        }

        List<GeneratedResource> resources = loadSearchResources(userId, request.getSpaceId(), spaceIds, fileIds.isEmpty());
        for (GeneratedResource resource : resources) {
            if (hasGeneratedResourceIndex(userId, resource.getId())) {
                continue;
            }
            String text = buildResourceText(resource);
            if (!StringUtils.hasText(text)) {
                continue;
            }
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source_type", "generated_resource");
            metadata.put("resource_id", resource.getId());
            metadata.put("resource_type", resource.getResourceType());
            metadata.put("space_id", resource.getSpaceId());
            metadata.put("retrieval_mode", retrievalMode());
            candidates.add(new SearchCandidate("生成资源：" + valueOrBlank(resource.getTitle()), 0, text, metadata));
        }

        List<String> tokens = queryTokens(request.getQuery());
        return candidates.stream()
                .map(candidate -> candidate.withScore(scoreCandidate(request.getQuery(), tokens, candidate)))
                .filter(candidate -> candidate.score > 0)
                .sorted(Comparator.comparingDouble(SearchCandidate::getScore).reversed())
                .limit(topK)
                .map(SearchCandidate::toItem)
                .toList();
    }

    private List<KnowledgeSearchResultVO.Item> findVectorResults(KnowledgeSearchRequest request, Long userId, int topK) {
        if (!"chroma".equalsIgnoreCase(ragVectorBackend)) {
            return Collections.emptyList();
        }
        try {
            Map<String, Object> filters = new LinkedHashMap<>();
            filters.put("user_id", userId);
            if (request.getSpaceId() != null) {
                filters.put("space_id", request.getSpaceId());
            }
            var response = aiServiceClient.searchKnowledge(AiRagSearchRequest.builder()
                    .query(request.getQuery())
                    .fileIds(request.getFileIds() == null ? Collections.emptyList() : request.getFileIds())
                    .topK(topK)
                    .filters(filters)
                    .build());
            if (response == null || !Boolean.TRUE.equals(response.getSuccess())) {
                return Collections.emptyList();
            }
            return nullSafe(response.getResults()).stream()
                    .filter(chunk -> StringUtils.hasText(chunk.getContentText()) && !looksLikeRawOfficeZip(chunk.getContentText()))
                    .map(this::toVectorItem)
                    .toList();
        } catch (RuntimeException exception) {
            return Collections.emptyList();
        }
    }

    private KnowledgeSearchResultVO.Item toVectorItem(AiRagChunkDTO chunk) {
        Map<String, Object> metadata = chunk.getMetadata() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(chunk.getMetadata());
        metadata.put("retrieval_mode", StringUtils.hasText(chunk.getRetrievalMode()) ? chunk.getRetrievalMode() : "vector");
        Long fileId = longValue(metadata.get("file_id"));
        String source = StringUtils.hasText(chunk.getSource()) ? chunk.getSource() : String.valueOf(metadata.getOrDefault("file_name", "知识片段"));
        String sourceFileName = StringUtils.hasText(chunk.getSourceFileName()) ? chunk.getSourceFileName() : source;
        return KnowledgeSearchResultVO.Item.builder()
                .fileId(fileId)
                .source(source)
                .sourceFileName(sourceFileName)
                .chunkIndex(chunk.getChunkIndex())
                .chunkText(chunk.getContentText())
                .score(chunk.getScore() == null ? BigDecimal.ZERO : chunk.getScore().setScale(2, RoundingMode.HALF_UP))
                .retrievalMode(String.valueOf(metadata.get("retrieval_mode")))
                .metadata(JsonUtils.fromJson(JsonUtils.toJson(metadata), JsonNode.class))
                .build();
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
    private String retrievalMode() {
        return "chroma".equalsIgnoreCase(ragVectorBackend) ? "mysql_fallback" : "mysql";
    }

    private List<KnowledgeFile> loadSearchFiles(Long userId, Long spaceId, List<Long> fileIds) {
        LambdaQueryWrapper<KnowledgeFile> wrapper = new LambdaQueryWrapper<KnowledgeFile>()
                .eq(KnowledgeFile::getUserId, userId)
                .eq(KnowledgeFile::getDeleted, 0)
                .eq(KnowledgeFile::getStatus, ACTIVE_STATUS)
                .eq(spaceId != null, KnowledgeFile::getSpaceId, spaceId)
                .in(fileIds != null && !fileIds.isEmpty(), KnowledgeFile::getId, fileIds);
        return knowledgeFileMapper.selectList(wrapper);
    }

    private List<GeneratedResource> loadSearchResources(Long userId, Long spaceId, Set<Long> spaceIds, boolean includeAllSpaces) {
        LambdaQueryWrapper<GeneratedResource> wrapper = new LambdaQueryWrapper<GeneratedResource>()
                .eq(GeneratedResource::getUserId, userId)
                .eq(GeneratedResource::getDeleted, 0)
                .eq(GeneratedResource::getStatus, ACTIVE_STATUS)
                .eq(spaceId != null, GeneratedResource::getSpaceId, spaceId)
                .in(spaceId == null && !includeAllSpaces && !spaceIds.isEmpty(), GeneratedResource::getSpaceId, spaceIds);
        return generatedResourceMapper.selectList(wrapper);
    }

    private boolean hasGeneratedResourceIndex(Long userId, Long resourceId) {
        return knowledgeFileMapper.selectCount(new LambdaQueryWrapper<KnowledgeFile>()
                .eq(KnowledgeFile::getUserId, userId)
                .eq(KnowledgeFile::getStoragePath, generatedResourceStoragePath(resourceId))
                .eq(KnowledgeFile::getDeleted, 0)) > 0;
    }

    private String buildResourceText(GeneratedResource resource) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(resource.getTitle())) {
            builder.append(resource.getTitle()).append('\n');
        }
        if (StringUtils.hasText(resource.getSubject())) {
            builder.append(resource.getSubject()).append('\n');
        }
        if (StringUtils.hasText(resource.getOutputSummary())) {
            builder.append(resource.getOutputSummary()).append('\n');
        }
        if (StringUtils.hasText(resource.getContentMarkdown())) {
            builder.append(resource.getContentMarkdown());
        }
        return builder.toString().trim();
    }

    private String generatedResourceStoragePath(Long resourceId) {
        return "generated-resource/" + resourceId + ".md";
    }

    private String generatedResourceName(GeneratedResource resource) {
        String title = StringUtils.hasText(resource.getTitle()) ? resource.getTitle() : "learning-resource-" + resource.getId();
        return "生成资源 - " + title.replaceAll("[\\\\/:*?\"<>|]+", "_") + ".md";
    }

    private double scoreCandidate(String query, List<String> tokens, SearchCandidate candidate) {
        String haystack = (candidate.source + "\n" + candidate.text).toLowerCase();
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
        double score = 0;
        if (StringUtils.hasText(normalizedQuery) && haystack.contains(normalizedQuery)) {
            score += 4.0;
        }
        for (String token : tokens) {
            int count = countOccurrences(haystack, token);
            if (count > 0) {
                score += Math.min(2.5, 0.7 + count * 0.25);
            }
        }
        if (score == 0) {
            return 0;
        }
        return Math.min(0.99, score / Math.max(5.0, tokens.size() + 3.0));
    }

    private List<String> queryTokens(String query) {
        if (!StringUtils.hasText(query)) {
            return Collections.emptyList();
        }
        Set<String> tokens = new HashSet<>();
        String lower = query.toLowerCase();
        Matcher wordMatcher = Pattern.compile("[a-z0-9_]{2,}").matcher(lower);
        while (wordMatcher.find()) {
            tokens.add(wordMatcher.group());
        }
        String cjkOnly = lower.replaceAll("[^\\p{IsHan}]+", "");
        for (int i = 0; i < cjkOnly.length(); i++) {
            if (cjkOnly.length() == 1) {
                tokens.add(String.valueOf(cjkOnly.charAt(i)));
            }
            if (i + 2 <= cjkOnly.length()) {
                tokens.add(cjkOnly.substring(i, i + 2));
            }
        }
        String trimmed = lower.trim();
        if (trimmed.length() >= 2 && trimmed.length() <= 80) {
            tokens.add(trimmed);
        }
        return new ArrayList<>(tokens);
    }

    private int countOccurrences(String text, String token) {
        if (!StringUtils.hasText(text) || !StringUtils.hasText(token)) {
            return 0;
        }
        int count = 0;
        int index = text.indexOf(token);
        while (index >= 0) {
            count++;
            index = text.indexOf(token, index + token.length());
        }
        return count;
    }

    private String buildGroundedAnswer(String query, List<KnowledgeSearchResultVO.Item> results) {
        if (results == null || results.isEmpty()) {
            return "没有在你已上传的资料或已生成资源中找到与“" + query + "”直接相关的内容。请先上传对应资料、重新整理资料，或生成相关学习资源后再提问。";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("根据你已上传的资料和已生成资源，找到以下依据：\n\n");
        int index = 1;
        for (KnowledgeSearchResultVO.Item item : results) {
            builder.append(index++).append(". **").append(item.getSource()).append("**：")
                    .append(compactSnippet(item.getChunkText(), 220)).append("\n");
        }
        builder.append("\n可以先围绕以上片段整理概念、关键步骤和易错点；如果要继续生成笔记或练习题，建议优先选择相关度最高的资料。");
        return builder.toString();
    }

    private AiRagChunkDTO toAiRagChunk(KnowledgeSearchResultVO.Item item) {
        AiRagChunkDTO chunk = new AiRagChunkDTO();
        String content = item.getChunkText() == null ? "" : item.getChunkText();
        String sourceKey = item.getFileId() == null ? "generated" : item.getFileId().toString();
        chunk.setChunkIndex(item.getChunkIndex());
        chunk.setContentText(content);
        chunk.setContentHash(sha256(content.getBytes(StandardCharsets.UTF_8)));
        chunk.setTokenCount(Math.max(1, (content.length() + 1) / 2));
        chunk.setEmbeddingRef("context://" + sourceKey + "/" + Objects.toString(item.getChunkIndex(), "0"));
        chunk.setScore(item.getScore());
        chunk.setSource(item.getSource());
        chunk.setSourceFileName(item.getSourceFileName());
        chunk.setRetrievalMode(item.getRetrievalMode());
        chunk.setMetadata(item.getMetadata() == null
                ? Map.of()
                : JsonUtils.fromJson(JsonUtils.toJson(item.getMetadata()), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}));
        return chunk;
    }

    private String compactSnippet(String text, int maxLength) {
        String compact = text == null ? "" : text.replaceAll("\\s+", " ").trim();
        if (compact.length() <= maxLength) {
            return compact;
        }
        return compact.substring(0, maxLength) + "...";
    }

    private boolean looksLikeRawOfficeZip(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        String trimmed = text.trim();
        return trimmed.startsWith("PK") && (trimmed.contains("word/") || trimmed.contains("docProps/") || trimmed.contains("ppt/"));
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

    private String resolveSourceText(KnowledgeFile file, KnowledgeIndexRequest request) {
        if (request != null && StringUtils.hasText(request.getSourceText())) {
            return request.getSourceText();
        }
        Path path = resolveStoredPath(file.getStoragePath());
        if (path != null && Files.exists(path)) {
            try {
                byte[] bytes = Files.readAllBytes(path);
                return textExtractor.extract(file.getOriginalName(), null, bytes);
            } catch (IOException exception) {
                throw new BusinessException(ResultCode.INTERNAL_ERROR, "读取已上传文件失败");
            }
        }
        return null;
    }

    private byte[] readBytes(MultipartFile multipartFile) {
        try {
            return multipartFile.getBytes();
        } catch (IOException exception) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "读取上传文件失败");
        }
    }

    private String saveFile(Long userId, Long spaceId, String originalName, byte[] bytes) {
        String day = LocalDate.now().toString().replace("-", "");
        String safeName = originalName.replaceAll("[\\\\/:*?\"<>|]+", "_");
        String relativePath = "knowledge/" + userId + "/" + spaceId + "/" + day + "/" + UUID.randomUUID() + "_" + safeName;
        Path root = Paths.get(storageDir).toAbsolutePath().normalize();
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件路径不合法");
        }
        try {
            Files.createDirectories(Objects.requireNonNull(target.getParent()));
            Files.write(target, bytes);
            return relativePath.replace("\\", "/");
        } catch (IOException exception) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "保存上传文件失败");
        }
    }

    private Path resolveStoredPath(String storagePath) {
        if (!StringUtils.hasText(storagePath)) {
            return null;
        }
        Path root = Paths.get(storageDir).toAbsolutePath().normalize();
        Path path = root.resolve(storagePath).normalize();
        return path.startsWith(root) ? path : null;
    }

    private String sanitizeOriginalName(String originalFilename) {
        String filename = StringUtils.hasText(originalFilename) ? originalFilename : "knowledge.txt";
        filename = Paths.get(filename).getFileName().toString().trim();
        if (!StringUtils.hasText(filename)) {
            filename = "knowledge.txt";
        }
        return filename.length() > 255 ? filename.substring(filename.length() - 255) : filename;
    }

    private String detectFileType(String originalName) {
        if (!StringUtils.hasText(originalName) || !originalName.contains(".")) {
            return "txt";
        }
        String extension = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
        return extension.length() > 32 ? extension.substring(0, 32) : extension;
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception exception) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "文件校验值计算失败");
        }
    }

    private KnowledgeChunkVO toChunkVO(KnowledgeChunk chunk) {
        return KnowledgeChunkVO.builder()
                .id(chunk.getId())
                .knowledgeFileId(chunk.getKnowledgeFileId())
                .chunkIndex(chunk.getChunkIndex())
                .contentText(chunk.getContentText())
                .contentHash(chunk.getContentHash())
                .tokenCount(chunk.getTokenCount())
                .metadata(chunk.getMetadata() == null ? null : JsonUtils.fromJson(chunk.getMetadata(), JsonNode.class))
                .embeddingRef(chunk.getEmbeddingRef())
                .status(chunk.getStatus())
                .build();
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

    private static class SearchCandidate {
        private final String source;
        private final Integer chunkIndex;
        private final String text;
        private final Map<String, Object> metadata;
        private final double score;

        private SearchCandidate(String source, Integer chunkIndex, String text, Map<String, Object> metadata) {
            this(source, chunkIndex, text, metadata, 0);
        }

        private SearchCandidate(String source, Integer chunkIndex, String text, Map<String, Object> metadata, double score) {
            this.source = source;
            this.chunkIndex = chunkIndex;
            this.text = text;
            this.metadata = metadata;
            this.score = score;
        }

        private SearchCandidate withScore(double score) {
            return new SearchCandidate(source, chunkIndex, text, metadata, score);
        }

        private double getScore() {
            return score;
        }

        private KnowledgeSearchResultVO.Item toItem() {
            return KnowledgeSearchResultVO.Item.builder()
                    .fileId(metadata.get("file_id") instanceof Long id ? id : null)
                    .source(source)
                    .sourceFileName(source)
                    .chunkIndex(chunkIndex)
                    .chunkText(text)
                    .score(BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP))
                    .retrievalMode(metadata.get("retrieval_mode") == null ? "mysql" : String.valueOf(metadata.get("retrieval_mode")))
                    .metadata(JsonUtils.fromJson(JsonUtils.toJson(metadata), JsonNode.class))
                    .build();
        }
    }

    private <T> List<T> nullSafe(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }

    private String valueOrBlank(String value) {
        return value == null ? "" : value;
    }
}
