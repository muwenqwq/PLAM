package com.edustudio.module.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class KnowledgeFileCreateRequest {

    @NotNull(message = "学习空间不能为空")
    private Long spaceId;

    @NotBlank(message = "文件名称不能为空")
    @Size(max = 255, message = "文件名称不能超过 255 个字符")
    private String originalName;

    @Size(max = 500, message = "文件路径不能超过 500 个字符")
    private String storagePath;

    @Size(max = 32, message = "文件类型不能超过 32 个字符")
    private String fileType = "md";

    private Long fileSize = 0L;

    private String sourceText;
}
