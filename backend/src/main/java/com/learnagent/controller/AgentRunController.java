package com.learnagent.controller;

import com.learnagent.dto.response.AgentRunResponse;
import com.learnagent.dto.response.ApiResult;
import com.learnagent.service.AgentRunService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/agent-runs")
@RequiredArgsConstructor
public class AgentRunController {

    private final AgentRunService agentRunService;

    @GetMapping("/{taskId}")
    public ApiResult<List<AgentRunResponse>> get(@PathVariable String taskId) {
        return ApiResult.ok(agentRunService.getAgentRuns(taskId));
    }
}
