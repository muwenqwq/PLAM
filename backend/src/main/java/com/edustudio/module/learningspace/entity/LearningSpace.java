package com.edustudio.module.learningspace.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.edustudio.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("learning_space")
public class LearningSpace extends BaseEntity {

    private Long userId;

    private String spaceName;

    private String subject;

    private String description;

    private String coverUrl;

    private String visibility;

    @TableField("is_default")
    private Boolean defaultSpace;

    private Integer resourceCount;

    private Integer taskCount;

    private String status;
}
