package com.learnagent.profile.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class ProfileResponse {
    private String taskId;
    private Long profileSnapshotId;
    private ProfileData profile;
    private LocalDateTime createdAt;

    @Data
    public static class ProfileData {
        private String major;
        private String grade;
        private String course;
        private String goal;
        private String foundation;
        private List<String> weakness;
        private List<String> preference;
        private String timeBudget;
        private Map<String, Double> masteryMap;
    }
}
