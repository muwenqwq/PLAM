package com.learnagent.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class AnalyticsResponse {
    private Long studentId;
    private Integer totalQuizzes;
    private Integer averageScore;
    private Map<String, Double> masteryMap;
    private List<String> weakPoints;
    private List<RecentResult> recentResults;

    @Data
    public static class RecentResult {
        private Long id;
        private String quizTitle;
        private Double score;
        private Double totalScore;
        private LocalDateTime submittedAt;
        private List<DetailItem> details;
    }

    @Data
    public static class DetailItem {
        private String question;
        private Boolean correct;
    }
}
