package com.edustudio.module.companion.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CompanionRoleUpdateRequest {

    @Size(max = 100, message = "角色名称不能超过 100 个字符")
    private String roleName;

    @Size(max = 100, message = "角色身份不能超过 100 个字符")
    private String roleIdentity;

    @Size(max = 512, message = "头像地址不能超过 512 个字符")
    private String avatarUrl;

    @Size(max = 32, message = "主题色不能超过 32 个字符")
    private String themeColor;

    @Size(max = 4000, message = "角色背景不能超过 4000 个字符")
    private String background;

    @Size(max = 4000, message = "角色性格不能超过 4000 个字符")
    private String personality;

    @Size(max = 500, message = "擅长内容不能超过 500 个字符")
    private String expertise;

    @Size(max = 500, message = "角色爱好不能超过 500 个字符")
    private String hobbies;

    @Size(max = 500, message = "说话风格不能超过 500 个字符")
    private String speakingStyle;

    @Size(max = 500, message = "互动场景不能超过 500 个字符")
    private String scenario;

    @Size(max = 500, message = "陪伴目标不能超过 500 个字符")
    private String companionGoal;

    @Size(max = 4000, message = "边界设置不能超过 4000 个字符")
    private String boundaries;

    @Size(max = 4000, message = "自定义提示词不能超过 4000 个字符")
    private String customPrompt;

    @Size(max = 500, message = "角色标签不能超过 500 个字符")
    private String tags;

    private Boolean defaultRole;

    @Pattern(regexp = "active|disabled", message = "状态只能是 active 或 disabled")
    private String status;
}
