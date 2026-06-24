package com.edustudio.module.resource.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResourceUpdateRequest {

    @NotBlank(message = "资源标题不能为空")
    @Size(max = 200, message = "资源标题不能超过 200 个字符")
    private String title;

    @Size(max = 64, message = "资源类型不能超过 64 个字符")
    private String resourceType;

    @Size(max = 128, message = "学科不能超过 128 个字符")
    private String subject;

    private String contentMarkdown;

    private JsonNode contentJson;

    private String outputSummary;

    private String status;
}
