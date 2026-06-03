package com.learnagent.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class TutorResponse {
    private String answer;
    private List<SourceItem> sources;
    private List<Long> suggestedResources;
    private List<AgentTraceItem> agentTrace;

    @Data
    public static class SourceItem {
        private String file;
        private String section;
    }
}
