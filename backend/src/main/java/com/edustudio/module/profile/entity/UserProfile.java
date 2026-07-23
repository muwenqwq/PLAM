package com.edustudio.module.profile.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.edustudio.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_profile")
public class UserProfile extends BaseEntity {

    private Long userId;

    private Long spaceId;

    private String realName;

    private String school;

    private String major;

    private String gradeLevel;

    private String learningGoal;

    private String profileNarrative;

    private String adaptiveSummary;

    private String subjectDirection;

    private String foundationLevel;

    private String interestTags;

    private String weakPoints;

    private String targetExam;

    private BigDecimal weeklyAvailableHours;

    private String availableTimeSlots;

    private String outputStyle;

    private String profileSource;

    private String lastActivitySource;

    private String lastActivitySummary;

    private LocalDateTime lastActivityAt;

    private String status;
}
