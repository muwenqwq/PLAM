package com.edustudio.module.knowledge.controller;

import com.edustudio.common.api.PageResult;
import com.edustudio.common.api.Result;
import com.edustudio.module.knowledge.dto.KnowledgeFileCreateRequest;
import com.edustudio.module.knowledge.dto.KnowledgeFileQueryRequest;
import com.edustudio.module.knowledge.dto.KnowledgeIndexRequest;
import com.edustudio.module.knowledge.dto.KnowledgeSearchRequest;
import com.edustudio.module.knowledge.service.KnowledgeService;
import com.edustudio.module.knowledge.vo.KnowledgeFileVO;
import com.edustudio.module.knowledge.vo.KnowledgeSearchResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "知识库")
@Validated
@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    @Operation(summary = "创建知识库文件元数据")
    @PostMapping("/files")
    public Result<KnowledgeFileVO> create(@Valid @RequestBody KnowledgeFileCreateRequest request) {
        return Result.success(knowledgeService.create(request));
    }

    @Operation(summary = "上传知识库文件并自动索引")
    @PostMapping(value = "/files/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<KnowledgeFileVO> upload(@RequestParam Long spaceId, @RequestPart("file") MultipartFile file) {
        return Result.success(knowledgeService.upload(spaceId, file));
    }

    @Operation(summary = "分页查询知识库文件")
    @GetMapping("/files")
    public Result<PageResult<KnowledgeFileVO>> page(@Valid KnowledgeFileQueryRequest request) {
        return Result.success(knowledgeService.page(request));
    }

    @Operation(summary = "查询知识库文件详情")
    @GetMapping("/files/{id}")
    public Result<KnowledgeFileVO> detail(@PathVariable Long id) {
        return Result.success(knowledgeService.detail(id));
    }

    @Operation(summary = "删除知识库文件")
    @DeleteMapping("/files/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        knowledgeService.delete(id);
        return Result.success();
    }

    @Operation(summary = "索引知识库文件")
    @PostMapping("/files/{id}/index")
    public Result<KnowledgeFileVO> index(@PathVariable Long id, @Valid @RequestBody KnowledgeIndexRequest request) {
        return Result.success(knowledgeService.index(id, request));
    }

    @Operation(summary = "检索知识库")
    @PostMapping("/search")
    public Result<KnowledgeSearchResultVO> search(@Valid @RequestBody KnowledgeSearchRequest request) {
        return Result.success(knowledgeService.search(request));
    }

    @Operation(summary = "知识库问答")
    @PostMapping("/qa")
    public Result<KnowledgeSearchResultVO> qa(@Valid @RequestBody KnowledgeSearchRequest request) {
        return Result.success(knowledgeService.qa(request));
    }
}
