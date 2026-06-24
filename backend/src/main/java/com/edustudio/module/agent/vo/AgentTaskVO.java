package com.edustudio.module.agent.vo;

import com.edustudio.module.resource.vo.GeneratedResourceVO;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentTaskVO {

    private Long id;
    private Long userId;
    private Long spaceId;
    private Long providerId;
    private String taskType;
    private String title;
    private JsonNode inputParams;
    private String executionStatus;
    private String outputSummary;
    private JsonNode resultJson;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<GeneratedResourceVO> resources;
}
