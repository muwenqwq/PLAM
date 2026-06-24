package com.edustudio.module.modelprovider.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ModelProviderQueryRequest {

    @Min(value = 1, message = "页码不能小于 1")
    private Long pageNum = 1L;

    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = 100, message = "每页条数不能超过 100")
    private Long pageSize = 10L;

    @Size(max = 128, message = "关键词不能超过 128 个字符")
    private String keyword;

    @Pattern(regexp = "mock|openai_compatible|deepseek|qwen|ollama|custom", message = "模型类型不合法")
    private String providerType;

    @Pattern(regexp = "active|disabled|test_failed", message = "状态只能是 active、disabled 或 test_failed")
    private String status;
}
