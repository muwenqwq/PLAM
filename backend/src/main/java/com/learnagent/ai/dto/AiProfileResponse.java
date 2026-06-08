package com.learnagent.ai.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class AiProfileResponse {
    private ProfileJson profileJson;
    private List<AgentTrace> agentTrace;

    @Data
    public static class ProfileJson {
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
