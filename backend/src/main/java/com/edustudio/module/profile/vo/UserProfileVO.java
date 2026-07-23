package com.edustudio.module.profile.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileVO {

    private Long id;

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

    private JsonNode interestTags;

    private JsonNode weakPoints;

    private String targetExam;

    private BigDecimal weeklyAvailableHours;

    private JsonNode availableTimeSlots;

    private String outputStyle;

    private String profileSource;

    private String lastActivitySource;

    private String lastActivitySummary;

    private LocalDateTime lastActivityAt;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static UserProfileVO empty(Long userId, Long spaceId) {
        return UserProfileVO.builder()
                .userId(userId)
                .spaceId(spaceId)
                .weeklyAvailableHours(BigDecimal.ZERO)
                .profileSource("manual")
                .status("incomplete")
                .build();
    }
}
