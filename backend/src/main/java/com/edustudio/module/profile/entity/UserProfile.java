package com.edustudio.module.profile.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.edustudio.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

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

    private String subjectDirection;

    private String foundationLevel;

    private String interestTags;

    private String weakPoints;

    private String targetExam;

    private BigDecimal weeklyAvailableHours;

    private String availableTimeSlots;

    private String outputStyle;

    private String profileSource;

    private String status;
}
