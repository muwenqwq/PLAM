package com.learnagent.agent.controller;

import com.learnagent.agent.dto.AgentRunResponse;
import com.learnagent.agent.service.AgentRunService;
import com.learnagent.common.dto.ApiResult;
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
