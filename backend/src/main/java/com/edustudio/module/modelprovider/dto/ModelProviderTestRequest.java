package com.edustudio.module.modelprovider.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ModelProviderTestRequest {

    @Size(max = 500, message = "测试提示词不能超过 500 个字符")
    private String prompt = "请用一句话介绍你能做什么。";
}
