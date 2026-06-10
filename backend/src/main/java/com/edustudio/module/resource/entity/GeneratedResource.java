package com.edustudio.module.resource.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.edustudio.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("generated_resource")
public class GeneratedResource extends BaseEntity {

    private Long userId;

    private Long spaceId;

    private Long taskId;

    private String resourceType;

    private String title;

    private String subject;

    private String knowledgePoints;

    private String contentMarkdown;

    private String contentJson;

    private String outputSummary;

    private BigDecimal qualityScore;

    private String exportStatus;

    private String status;
}
