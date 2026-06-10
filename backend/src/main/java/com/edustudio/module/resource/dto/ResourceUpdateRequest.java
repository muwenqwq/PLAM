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

    private String contentMarkdown;

    private JsonNode contentJson;

    private String outputSummary;

    private String status;
}
