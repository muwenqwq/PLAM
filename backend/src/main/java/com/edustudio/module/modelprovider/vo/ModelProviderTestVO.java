package com.edustudio.module.modelprovider.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelProviderTestVO {

    private Long providerId;

    private Boolean success;

    private String providerType;

    private String modelName;

    private Integer latencyMs;

    private String message;

    private String sampleOutput;
}
