package com.edustudio.module.modelprovider.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.edustudio.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_model_provider")
public class AiModelProvider extends BaseEntity {

    private Long userId;

    private String providerName;

    private String providerType;

    private String baseUrl;

    private String apiKeyEncrypted;

    private String apiKeyMasked;

    private String modelName;

    private String embeddingModel;

    private BigDecimal temperature;

    private Integer maxTokens;

    private Boolean streamEnabled;

    @TableField("is_default")
    private Boolean defaultProvider;

    private String status;

    private String remark;
}
