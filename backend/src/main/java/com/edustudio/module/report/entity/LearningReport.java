package com.edustudio.module.report.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.edustudio.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("learning_report")
public class LearningReport extends BaseEntity {

    private Long userId;
    private Long spaceId;
    private String reportType;
    private String title;
    private String summary;
    private String reportJson;
    private String chartDataJson;
    private String suggestionText;
    private String status;
}
