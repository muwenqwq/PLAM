package com.edustudio.integration.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AiProfileAnalyzeResponse {

    private Boolean success;

    @JsonProperty("should_update")
    private Boolean shouldUpdate;

    private BigDecimal confidence;

    @JsonProperty("profile_narrative")
    private String profileNarrative;

    @JsonProperty("learning_goal")
    private String learningGoal;

    @JsonProperty("subject_direction")
    private String subjectDirection;

    @JsonProperty("foundation_level")
    private String foundationLevel;

    @JsonProperty("interest_tags")
    private List<String> interestTags;

    @JsonProperty("weak_points")
    private List<String> weakPoints;

    @JsonProperty("weekly_available_hours")
    private BigDecimal weeklyAvailableHours;

    @JsonProperty("available_time_slots")
    private List<String> availableTimeSlots;

    @JsonProperty("output_style")
    private String outputStyle;

    @JsonProperty("adaptive_summary")
    private String adaptiveSummary;

    @JsonProperty("evidence_summary")
    private String evidenceSummary;
}
