package com.edustudio.integration.ai.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiResourceGenerateResponse {

    private Boolean success;

    private List<AiResourceDTO> resources;
}
