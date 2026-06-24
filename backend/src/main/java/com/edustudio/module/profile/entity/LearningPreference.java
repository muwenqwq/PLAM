package com.edustudio.module.profile.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.edustudio.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("learning_preference")
public class LearningPreference extends BaseEntity {

    private Long userId;

    private String preferredResourceTypes;

    private String outputStyle;

    private String contentLengthPreference;

    private String difficultyPreference;

    private String languagePreference;

    private String studyTimeSlots;

    private Boolean notificationEnabled;

    private Boolean knowledgeGraphEnabled;

    private Boolean quizEnabled;

    private Boolean reviewPlanEnabled;

    private String status;
}
