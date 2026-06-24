package com.edustudio.module.resource.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResourceGenerateResultVO {

    private GeneratedResourceVO resource;

    private String message;
}
