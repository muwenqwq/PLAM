package com.edustudio.module.knowledge.service;

import com.edustudio.common.api.PageResult;
import com.edustudio.module.knowledge.dto.KnowledgeFileCreateRequest;
import com.edustudio.module.knowledge.dto.KnowledgeFileQueryRequest;
import com.edustudio.module.knowledge.dto.KnowledgeIndexRequest;
import com.edustudio.module.knowledge.dto.KnowledgeSearchRequest;
import com.edustudio.module.knowledge.vo.KnowledgeFileVO;
import com.edustudio.module.knowledge.vo.KnowledgeSearchResultVO;
import com.edustudio.module.resource.entity.GeneratedResource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface KnowledgeService {

    KnowledgeFileVO create(KnowledgeFileCreateRequest request);

    KnowledgeFileVO upload(Long spaceId, MultipartFile file);

    PageResult<KnowledgeFileVO> page(KnowledgeFileQueryRequest request);

    KnowledgeFileVO detail(Long id);

    void delete(Long id);

    KnowledgeFileVO index(Long id, KnowledgeIndexRequest request);

    KnowledgeSearchResultVO search(KnowledgeSearchRequest request);

    KnowledgeSearchResultVO qa(KnowledgeSearchRequest request);

    KnowledgeSearchResultVO generationContext(Long spaceId, List<Long> fileIds, Integer topK);

    KnowledgeFileVO syncGeneratedResource(GeneratedResource resource);

    void deleteGeneratedResourceIndex(Long resourceId, Long userId);
}
