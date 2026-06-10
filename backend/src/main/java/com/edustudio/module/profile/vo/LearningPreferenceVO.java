package com.edustudio.module.profile.vo;

import com.edustudio.common.utils.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class LearningPreferenceVO {

    private Long id;

    private Long userId;

    private JsonNode preferredResourceTypes;

    private String outputStyle;

    private String contentLengthPreference;

    private String difficultyPreference;

    private String languagePreference;

    private JsonNode studyTimeSlots;

    private Boolean notificationEnabled;

    private Boolean knowledgeGraphEnabled;

    private Boolean quizEnabled;

    private Boolean reviewPlanEnabled;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static LearningPreferenceVO defaults(Long userId) {
        return LearningPreferenceVO.builder()
                .userId(userId)
                .preferredResourceTypes(JsonUtils.fromJson("[\"学习计划\",\"复习提纲\",\"习题集\",\"知识图谱\"]", JsonNode.class))
                .outputStyle("markdown")
                .contentLengthPreference("medium")
                .difficultyPreference("medium")
                .languagePreference("zh-CN")
                .studyTimeSlots(JsonUtils.fromJson("[]", JsonNode.class))
                .notificationEnabled(true)
                .knowledgeGraphEnabled(true)
                .quizEnabled(true)
                .reviewPlanEnabled(true)
                .status("active")
                .build();
    }
}
