package com.edustudio.integration.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiModelConfigDTO {

    private String providerName;

    private String providerType;

    private String baseUrl;

    private String apiKey;

    private String modelName;

    private String embeddingModel;

    private BigDecimal temperature;

    private Integer maxTokens;

    private Boolean streamEnabled;

    private Map<String, Object> extra;
}
