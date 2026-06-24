package com.edustudio.module.knowledge.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class KnowledgeFileQueryRequest {

    private Long spaceId;
    private String parserStatus;
    private String keyword;

    @Min(value = 1, message = "页码不能小于 1")
    private long pageNum = 1;

    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = 100, message = "每页条数不能超过 100")
    private long pageSize = 10;
}
