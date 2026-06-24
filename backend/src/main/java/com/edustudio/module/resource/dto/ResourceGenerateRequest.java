package com.edustudio.module.resource.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ResourceGenerateRequest {

    @NotNull(message = "学习空间不能为空")
    private Long spaceId;

    private Long modelProviderId;

    @NotBlank(message = "资源标题不能为空")
    @Size(max = 200, message = "资源标题不能超过 200 个字符")
    private String title;

    @Size(max = 128, message = "学科不能超过 128 个字符")
    private String subject;

    @NotBlank(message = "资源类型不能为空")
    private String resourceType = "plan";

    private JsonNode knowledgePoints;

    private String difficulty = "medium";

    private String outputLength = "medium";

    private Boolean useKnowledgeBase = false;

    private List<Long> sourceFileIds = new ArrayList<>();

    private List<String> sourceFileNames = new ArrayList<>();
}
