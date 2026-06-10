package com.edustudio.module.profile.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserProfileUpsertRequest {

    @Size(max = 64, message = "真实姓名不能超过 64 个字符")
    private String realName;

    @Size(max = 128, message = "学校名称不能超过 128 个字符")
    private String school;

    @Size(max = 128, message = "专业名称不能超过 128 个字符")
    private String major;

    @Size(max = 64, message = "年级不能超过 64 个字符")
    private String gradeLevel;

    @Size(max = 1000, message = "学习目标不能超过 1000 个字符")
    private String learningGoal;

    @Size(max = 128, message = "学科方向不能超过 128 个字符")
    private String subjectDirection;

    @Pattern(regexp = "beginner|intermediate|advanced", message = "基础水平只能是 beginner、intermediate 或 advanced")
    private String foundationLevel;

    private JsonNode interestTags;

    private JsonNode weakPoints;

    @Size(max = 128, message = "目标考试不能超过 128 个字符")
    private String targetExam;

    @DecimalMin(value = "0.0", message = "每周可用学习时间不能小于 0")
    private BigDecimal weeklyAvailableHours;

    private JsonNode availableTimeSlots;

    @Size(max = 64, message = "输出风格不能超过 64 个字符")
    private String outputStyle;

    @Pattern(regexp = "manual|chat|assessment", message = "画像来源只能是 manual、chat 或 assessment")
    private String profileSource;
}
