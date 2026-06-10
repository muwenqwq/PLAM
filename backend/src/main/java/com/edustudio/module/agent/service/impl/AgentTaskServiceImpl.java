package com.edustudio.module.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edustudio.common.api.PageResult;
import com.edustudio.common.api.ResultCode;
import com.edustudio.common.exception.BusinessException;
import com.edustudio.common.security.LoginUserHolder;
import com.edustudio.common.utils.JsonUtils;
import com.edustudio.integration.ai.AiServiceClient;
import com.edustudio.integration.ai.dto.AiAgentRunRequest;
import com.edustudio.integration.ai.dto.AiAgentRunResponse;
import com.edustudio.integration.ai.dto.AiAgentStepDTO;
import com.edustudio.integration.ai.dto.AiResourceDTO;
import com.edustudio.module.agent.dto.AgentTaskCreateRequest;
import com.edustudio.module.agent.dto.AgentTaskQueryRequest;
import com.edustudio.module.agent.entity.AgentStep;
import com.edustudio.module.agent.entity.AgentTask;
import com.edustudio.module.agent.mapper.AgentStepMapper;
import com.edustudio.module.agent.mapper.AgentTaskMapper;
import com.edustudio.module.agent.service.AgentTaskService;
import com.edustudio.module.agent.vo.AgentStepVO;
import com.edustudio.module.agent.vo.AgentTaskResultVO;
import com.edustudio.module.agent.vo.AgentTaskVO;
import com.edustudio.module.learningspace.service.LearningSpaceService;
import com.edustudio.module.modelprovider.service.ModelProviderService;
import com.edustudio.module.resource.entity.GeneratedResource;
import com.edustudio.module.resource.mapper.GeneratedResourceMapper;
import com.edustudio.module.resource.service.GeneratedResourceService;
import com.edustudio.module.resource.vo.GeneratedResourceVO;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentTaskServiceImpl implements AgentTaskService {

    private static final String ACTIVE_STATUS = "active";

    private final AgentTaskMapper agentTaskMapper;
    private final AgentStepMapper agentStepMapper;
    private final GeneratedResourceMapper generatedResourceMapper;
    private final GeneratedResourceService generatedResourceService;
    private final LearningSpaceService learningSpaceService;
    private final ModelProviderService modelProviderService;
    private final AiServiceClient aiServiceClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentTaskResultVO createAndRun(AgentTaskCreateRequest request) {
        Long userId = LoginUserHolder.requireCurrentUserId();
        if (request.getSpaceId() != null) {
            learningSpaceService.assertOwned(request.getSpaceId());
        }
        Long providerId = modelProviderService.resolveProviderId(request.getProviderId());

        AgentTask task = new AgentTask();
        task.setUserId(userId);
        task.setSpaceId(request.getSpaceId());
        task.setProviderId(providerId);
        task.setTaskType(request.getTaskType());
        task.setTitle(request.getTitle());
        task.setInputParams(request.getInputParams() == null ? "{}" : JsonUtils.toJson(request.getInputParams()));
        task.setExecutionStatus("running");
        task.setStartedAt(LocalDateTime.now());
        task.setStatus(ACTIVE_STATUS);
        agentTaskMapper.insert(task);

        return runAndPersist(task, request.getSubject(), request.getResourceType());
    }

    @Override
    public PageResult<AgentTaskVO> page(AgentTaskQueryRequest request) {
        Long userId = LoginUserHolder.requireCurrentUserId();
        LambdaQueryWrapper<AgentTask> wrapper = new LambdaQueryWrapper<AgentTask>()
                .eq(AgentTask::getUserId, userId)
                .eq(AgentTask::getDeleted, 0)
                .eq(request.getSpaceId() != null, AgentTask::getSpaceId, request.getSpaceId())
                .eq(StringUtils.hasText(request.getExecutionStatus()), AgentTask::getExecutionStatus, request.getExecutionStatus())
                .eq(StringUtils.hasText(request.getTaskType()), AgentTask::getTaskType, request.getTaskType())
                .like(StringUtils.hasText(request.getKeyword()), AgentTask::getTitle, request.getKeyword())
                .orderByDesc(AgentTask::getCreatedAt);
        Page<AgentTask> result = agentTaskMapper.selectPage(new Page<>(request.getPageNum(), request.getPageSize()), wrapper);
        return PageResult.of(result.getRecords().stream().map(this::toTaskVO).toList(),
                result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public AgentTaskVO detail(Long id) {
        return toTaskVO(getOwnedTask(id));
    }

    @Override
    public List<AgentStepVO> steps(Long id) {
        getOwnedTask(id);
        return agentStepMapper.selectList(new LambdaQueryWrapper<AgentStep>()
                        .eq(AgentStep::getTaskId, id)
                        .eq(AgentStep::getUserId, LoginUserHolder.requireCurrentUserId())
                        .eq(AgentStep::getDeleted, 0)
                        .orderByAsc(AgentStep::getStepOrder))
                .stream()
                .map(this::toStepVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentTaskResultVO rerun(Long id) {
        AgentTask task = getOwnedTask(id);
        task.setExecutionStatus("running");
        task.setErrorMessage(null);
        task.setStartedAt(LocalDateTime.now());
        task.setFinishedAt(null);
        agentTaskMapper.updateById(task);
        return runAndPersist(task, null, "plan");
    }

    @Override
    public GeneratedResourceVO saveResource(Long id) {
        getOwnedTask(id);
        GeneratedResource resource = generatedResourceMapper.selectOne(new LambdaQueryWrapper<GeneratedResource>()
                .eq(GeneratedResource::getTaskId, id)
                .eq(GeneratedResource::getUserId, LoginUserHolder.requireCurrentUserId())
                .eq(GeneratedResource::getDeleted, 0)
                .orderByDesc(GeneratedResource::getCreatedAt)
                .last("LIMIT 1"));
        if (resource == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "当前任务还没有可保存的生成资源");
        }
        return generatedResourceService.toVO(resource);
    }

    private AgentTaskResultVO runAndPersist(AgentTask task, String subject, String resourceType) {
        try {
            AiAgentRunResponse response = aiServiceClient.runAgents(AiAgentRunRequest.builder()
                    .modelConfig(modelProviderService.resolveConfig(task.getProviderId()))
                    .userId(task.getUserId())
                    .spaceId(task.getSpaceId())
                    .providerId(task.getProviderId())
                    .taskType(task.getTaskType())
                    .title(task.getTitle())
                    .subject(subject)
                    .resourceType(resourceType)
                    .inputParams(parseJson(task.getInputParams()))
                    .build());

            List<AgentStepVO> stepVOs = persistSteps(task, nullSafe(response.getSteps()));
            List<GeneratedResourceVO> resourceVOs = persistResources(task, nullSafe(response.getResources()));
            task.setExecutionStatus(StringUtils.hasText(response.getExecutionStatus())
                    ? response.getExecutionStatus()
                    : "succeeded");
            task.setOutputSummary(response.getOutputSummary());
            task.setResultJson(response.getResultJson() == null ? "{}" : JsonUtils.toJson(response.getResultJson()));
            task.setErrorMessage(response.getErrorMessage());
            task.setFinishedAt(LocalDateTime.now());
            agentTaskMapper.updateById(task);
            return AgentTaskResultVO.builder()
                    .task(toTaskVO(task))
                    .steps(stepVOs)
                    .resources(resourceVOs)
                    .build();
        } catch (BusinessException exception) {
            task.setExecutionStatus("failed");
            task.setErrorMessage(exception.getMessage());
            task.setFinishedAt(LocalDateTime.now());
            agentTaskMapper.updateById(task);
            throw exception;
        }
    }

    private List<AgentStepVO> persistSteps(AgentTask task, List<AiAgentStepDTO> steps) {
        return steps.stream().map(stepDTO -> {
            AgentStep step = new AgentStep();
            step.setTaskId(task.getId());
            step.setUserId(task.getUserId());
            step.setAgentName(stepDTO.getAgentName());
            step.setStepOrder(stepDTO.getStepOrder());
            step.setStepType(stepDTO.getStepType());
            step.setExecutionStatus(stepDTO.getExecutionStatus());
            step.setInputJson(stepDTO.getInputJson() == null ? "{}" : JsonUtils.toJson(stepDTO.getInputJson()));
            step.setOutputSummary(stepDTO.getOutputSummary());
            step.setResultJson(stepDTO.getResultJson() == null ? "{}" : JsonUtils.toJson(stepDTO.getResultJson()));
            step.setErrorMessage(stepDTO.getErrorMessage());
            step.setStartedAt(LocalDateTime.now());
            step.setFinishedAt(LocalDateTime.now());
            step.setStatus(ACTIVE_STATUS);
            agentStepMapper.insert(step);
            return toStepVO(step);
        }).toList();
    }

    private List<GeneratedResourceVO> persistResources(AgentTask task, List<AiResourceDTO> resources) {
        return resources.stream().map(resourceDTO -> {
            GeneratedResource resource = new GeneratedResource();
            resource.setUserId(task.getUserId());
            resource.setSpaceId(task.getSpaceId());
            resource.setTaskId(task.getId());
            resource.setResourceType(resourceDTO.getResourceType());
            resource.setTitle(resourceDTO.getTitle());
            resource.setSubject(resourceDTO.getSubject());
            resource.setKnowledgePoints(resourceDTO.getKnowledgePoints() == null ? "[]" : JsonUtils.toJson(resourceDTO.getKnowledgePoints()));
            resource.setContentMarkdown(resourceDTO.getContentMarkdown());
            resource.setContentJson(resourceDTO.getContentJson() == null ? "{}" : JsonUtils.toJson(resourceDTO.getContentJson()));
            resource.setOutputSummary(resourceDTO.getOutputSummary());
            resource.setQualityScore(resourceDTO.getQualityScore());
            resource.setExportStatus("markdown");
            resource.setStatus(ACTIVE_STATUS);
            generatedResourceMapper.insert(resource);
            return generatedResourceService.toVO(resource);
        }).toList();
    }

    private AgentTask getOwnedTask(Long id) {
        AgentTask task = agentTaskMapper.selectOne(new LambdaQueryWrapper<AgentTask>()
                .eq(AgentTask::getId, id)
                .eq(AgentTask::getUserId, LoginUserHolder.requireCurrentUserId())
                .eq(AgentTask::getDeleted, 0));
        if (task == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Agent 任务不存在或无权访问");
        }
        return task;
    }

    private AgentTaskVO toTaskVO(AgentTask task) {
        return AgentTaskVO.builder()
                .id(task.getId())
                .userId(task.getUserId())
                .spaceId(task.getSpaceId())
                .providerId(task.getProviderId())
                .taskType(task.getTaskType())
                .title(task.getTitle())
                .inputParams(parseJson(task.getInputParams()))
                .executionStatus(task.getExecutionStatus())
                .outputSummary(task.getOutputSummary())
                .resultJson(parseJson(task.getResultJson()))
                .errorMessage(task.getErrorMessage())
                .startedAt(task.getStartedAt())
                .finishedAt(task.getFinishedAt())
                .status(task.getStatus())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    private AgentStepVO toStepVO(AgentStep step) {
        return AgentStepVO.builder()
                .id(step.getId())
                .taskId(step.getTaskId())
                .userId(step.getUserId())
                .agentName(step.getAgentName())
                .stepOrder(step.getStepOrder())
                .stepType(step.getStepType())
                .executionStatus(step.getExecutionStatus())
                .inputJson(parseJson(step.getInputJson()))
                .outputSummary(step.getOutputSummary())
                .resultJson(parseJson(step.getResultJson()))
                .errorMessage(step.getErrorMessage())
                .startedAt(step.getStartedAt())
                .finishedAt(step.getFinishedAt())
                .status(step.getStatus())
                .build();
    }

    private JsonNode parseJson(String json) {
        return StringUtils.hasText(json) ? JsonUtils.fromJson(json, JsonNode.class) : null;
    }

    private <T> List<T> nullSafe(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }
}
