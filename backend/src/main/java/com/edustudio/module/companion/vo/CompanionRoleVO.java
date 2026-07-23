package com.edustudio.module.companion.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanionRoleVO {

    private Long id;

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

    private Boolean defaultRole;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
