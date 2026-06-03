package com.learnagent.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.learnagent.dto.response.AgentRunResponse;
import com.learnagent.entity.AgentRun;
import com.learnagent.mapper.AgentRunMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentRunService {

    private final AgentRunMapper agentRunMapper;

    public List<AgentRunResponse> getAgentRuns(String taskId) {
        List<AgentRun> runs = agentRunMapper.selectList(
                new QueryWrapper<AgentRun>()
                        .eq("task_id", taskId)
                        .orderByAsc("created_at"));
        return runs.stream().map(this::toAgentRunResponse).collect(Collectors.toList());
    }

    private AgentRunResponse toAgentRunResponse(AgentRun run) {
        AgentRunResponse resp = new AgentRunResponse();
        resp.setId(run.getId());
        resp.setTaskId(run.getTaskId());
        resp.setAgentName(run.getAgentName());
        resp.setInputSummary(run.getInputSummary());
        resp.setOutputSummary(run.getOutputSummary());
        resp.setModelName(run.getModelName());
        resp.setStatus(run.getStatus());
        resp.setLatencyMs(run.getLatencyMs());
        resp.setErrorMessage(run.getErrorMessage());
        resp.setCreatedAt(run.getCreatedAt());
        return resp;
    }
}
