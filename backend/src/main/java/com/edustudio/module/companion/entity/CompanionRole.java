package com.edustudio.module.companion.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.edustudio.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_companion_role")
public class CompanionRole extends BaseEntity {

    private Long userId;

    private String roleName;

    private String roleIdentity;

    private String avatarUrl;

    private String themeColor;

    private String background;

    private String personality;

    private String expertise;

    private String hobbies;

    private String speakingStyle;

    private String scenario;

    private String companionGoal;

    private String boundaries;

    private String customPrompt;

    private String tags;

    @TableField("is_default")
    private Boolean defaultRole;

    private String status;
}
