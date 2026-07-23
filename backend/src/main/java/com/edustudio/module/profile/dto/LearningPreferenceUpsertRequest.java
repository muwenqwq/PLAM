package com.edustudio.module.profile.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class LearningPreferenceUpsertRequest {

    private JsonNode preferredResourceTypes;

    @Pattern(regexp = "markdown|structured|concise|detailed", message = "输出风格只能是 markdown、structured、concise 或 detailed")
    private String outputStyle;

    @Pattern(regexp = "short|medium|long", message = "内容长度偏好只能是 short、medium 或 long")
    private String contentLengthPreference;

    @Pattern(regexp = "easy|medium|hard|adaptive", message = "难度偏好只能是 easy、medium、hard 或 adaptive")
    private String difficultyPreference;

    @Pattern(regexp = "zh-CN", message = "系统仅支持中文输出")
    private String languagePreference;

    private JsonNode studyTimeSlots;

    private Boolean notificationEnabled;

    private Boolean knowledgeGraphEnabled;

    private Boolean quizEnabled;

    private Boolean reviewPlanEnabled;
}
