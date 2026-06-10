package com.edustudio.module.modelprovider.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelProviderVO {

    private Long id;

    private Long userId;

    private String providerName;

    private String providerType;

    private String baseUrl;

    private String apiKeyMasked;

    private String modelName;

    private String embeddingModel;

    private BigDecimal temperature;

    private Integer maxTokens;

    private Boolean streamEnabled;

    @JsonProperty("isDefault")
    private Boolean defaultProvider;

    private String status;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
