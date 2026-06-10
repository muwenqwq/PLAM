package com.edustudio.module.modelprovider.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ModelProviderUpdateRequest {

    @Size(max = 128, message = "模型配置名称不能超过 128 个字符")
    private String providerName;

    @Pattern(regexp = "mock|openai_compatible|deepseek|qwen|ollama|custom", message = "模型类型不合法")
    private String providerType;

    @Size(max = 512, message = "baseUrl 不能超过 512 个字符")
    private String baseUrl;

    @Size(max = 1024, message = "API Key 不能超过 1024 个字符")
    private String apiKey;

    @Size(max = 128, message = "模型名称不能超过 128 个字符")
    private String modelName;

    @Size(max = 128, message = "Embedding 模型名称不能超过 128 个字符")
    private String embeddingModel;

    @DecimalMin(value = "0.0", message = "temperature 不能小于 0")
    @DecimalMax(value = "2.0", message = "temperature 不能大于 2")
    private BigDecimal temperature;

    @Min(value = 1, message = "maxTokens 不能小于 1")
    @Max(value = 200000, message = "maxTokens 不能超过 200000")
    private Integer maxTokens;

    private Boolean streamEnabled;

    @Pattern(regexp = "active|disabled|test_failed", message = "状态只能是 active、disabled 或 test_failed")
    private String status;

    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;
}
