package com.edustudio.module.agent.controller;

import com.edustudio.common.api.PageResult;
import com.edustudio.common.api.Result;
import com.edustudio.module.agent.dto.AgentTaskCreateRequest;
import com.edustudio.module.agent.dto.AgentTaskQueryRequest;
import com.edustudio.module.agent.service.AgentTaskService;
import com.edustudio.module.agent.vo.AgentStepVO;
import com.edustudio.module.agent.vo.AgentTaskResultVO;
import com.edustudio.module.agent.vo.AgentTaskVO;
import com.edustudio.module.resource.vo.GeneratedResourceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "多智能体任务")
@Validated
@RestController
@RequestMapping("/api/agent-tasks")
@RequiredArgsConstructor
public class AgentTaskController {

    private final AgentTaskService agentTaskService;

    @Operation(summary = "创建并同步执行 Agent 任务")
    @PostMapping
    public Result<AgentTaskResultVO> create(@Valid @RequestBody AgentTaskCreateRequest request) {
        return Result.success(agentTaskService.createAndRun(request));
    }

    @Operation(summary = "分页查询 Agent 任务")
    @GetMapping
    public Result<PageResult<AgentTaskVO>> page(@Valid AgentTaskQueryRequest request) {
        return Result.success(agentTaskService.page(request));
    }

    @Operation(summary = "查询 Agent 任务详情")
    @GetMapping("/{id}")
    public Result<AgentTaskVO> detail(@PathVariable Long id) {
        return Result.success(agentTaskService.detail(id));
    }

    @Operation(summary = "查询 Agent 执行步骤")
    @GetMapping("/{id}/steps")
    public Result<List<AgentStepVO>> steps(@PathVariable Long id) {
        return Result.success(agentTaskService.steps(id));
    }

    @Operation(summary = "重新执行 Agent 任务")
    @PostMapping("/{id}/rerun")
    public Result<AgentTaskResultVO> rerun(@PathVariable Long id) {
        return Result.success(agentTaskService.rerun(id));
    }

    @Operation(summary = "保存或返回任务生成资源")
    @PostMapping("/{id}/save-resource")
    public Result<GeneratedResourceVO> saveResource(@PathVariable Long id) {
        return Result.success(agentTaskService.saveResource(id));
    }
}
