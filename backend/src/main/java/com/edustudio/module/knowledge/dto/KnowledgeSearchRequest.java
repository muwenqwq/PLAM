package com.edustudio.module.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class KnowledgeSearchRequest {

    private Long spaceId;

    private List<Long> fileIds = new ArrayList<>();

    @NotBlank(message = "检索问题不能为空")
    private String query;

    @Min(value = 1, message = "topK 不能小于 1")
    @Max(value = 20, message = "topK 不能超过 20")
    private Integer topK = 5;
}
