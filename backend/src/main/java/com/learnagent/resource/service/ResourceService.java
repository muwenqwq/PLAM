package com.learnagent.resource.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnagent.ai.AiServiceClient;
import com.learnagent.ai.dto.AgentTrace;
import com.learnagent.ai.dto.AiResourceRequest;
import com.learnagent.ai.dto.AiResourceResponse;
import com.learnagent.resource.dto.ResourceGenerateRequest;
import com.learnagent.resource.dto.ResourceResponse;
import com.learnagent.common.entity.AgentRun;
import com.learnagent.common.entity.AiTask;
import com.learnagent.common.entity.KnowledgePoint;
import com.learnagent.common.entity.ProfileSnapshot;
import com.learnagent.common.entity.Resource;
import com.learnagent.common.mapper.AgentRunMapper;
import com.learnagent.common.mapper.AiTaskMapper;
import com.learnagent.common.mapper.KnowledgePointMapper;
import com.learnagent.common.mapper.ProfileSnapshotMapper;
import com.learnagent.common.mapper.ResourceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceMapper resourceMapper;
    private final AiTaskMapper aiTaskMapper;
    private final AgentRunMapper agentRunMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final ProfileSnapshotMapper profileSnapshotMapper;
    private final AiServiceClient aiServiceClient;
    private final ObjectMapper objectMapper;

    public Map<String, Object> generateResources(ResourceGenerateRequest req) {
        String taskId = "task_" + System.currentTimeMillis();

        // 创建 AiTask
        AiTask task = new AiTask();
        task.setTaskId(taskId);
        task.setStudentId(req.getStudentId());
        task.setCourseId(req.getCourseId());
        task.setTaskType("resource_generate");
        task.setStatus("created");
        task.setStartedAt(LocalDateTime.now());
        aiTaskMapper.insert(task);

        // 查询知识点名称
        KnowledgePoint kp = knowledgePointMapper.selectById(req.getKnowledgePointId());
        String kpName = kp != null ? kp.getName() : "";

        // 查询最新 profile
        ProfileSnapshot ps = profileSnapshotMapper.selectOne(
                new QueryWrapper<ProfileSnapshot>()
                        .eq("student_id", req.getStudentId())
                        .orderByDesc("created_at")
                        .last("LIMIT 1"));

        Map<String, Object> profileMap = new HashMap<>();
        if (ps != null) {
            try {
                profileMap = objectMapper.readValue(ps.getProfileJson(),
                        new TypeReference<Map<String, Object>>() {});
            } catch (JsonProcessingException e) {
                log.error("解析 profileJson 失败", e);
            }
        }

        // 构建请求，调用 AI 服务
        AiResourceRequest aiReq = new AiResourceRequest();
        aiReq.setTaskId(taskId);
        aiReq.setStudentId(req.getStudentId());
        aiReq.setCourseId(req.getCourseId());
        aiReq.setKnowledgePointId(req.getKnowledgePointId());
        aiReq.setKnowledgePointName(kpName);
        aiReq.setProfileJson(profileMap);
        aiReq.setResourceTypes(req.getResourceTypes());
        aiReq.setCourseChunks(Collections.emptyList());  // 必填，否则 Python 422

        AiResourceResponse aiResp = aiServiceClient.generateResources(aiReq);

        // 保存 Resource
        List<Long> resourceIds = new ArrayList<>();
        List<AiResourceResponse.ResourceItem> resources = aiResp.getResources();
        List<AiResourceResponse.ReviewResult.ReviewDetail> details =
                aiResp.getReviewResult() != null && aiResp.getReviewResult().getDetails() != null
                        ? aiResp.getReviewResult().getDetails() : Collections.emptyList();

        for (int i = 0; i < resources.size(); i++) {
            AiResourceResponse.ResourceItem item = resources.get(i);
            Resource res = new Resource();
            res.setStudentId(req.getStudentId());
            res.setCourseId(req.getCourseId());
            res.setKnowledgePointId(req.getKnowledgePointId());
            res.setTaskId(taskId);
            res.setResourceType(item.getResourceType());
            res.setTitle(item.getTitle());
            res.setContent(item.getContent());
            res.setFormat(item.getFormat());
            res.setStatus("completed");

            // 设置 qualityScore
            if (i < details.size()) {
                Double score = details.get(i).getQualityScore();
                res.setQualityScore(score != null ? BigDecimal.valueOf(score) : null);
            }

            // 序列化 sourcesJson
            if (item.getSourcesJson() != null) {
                try {
                    res.setSourcesJson(objectMapper.writeValueAsString(item.getSourcesJson()));
                } catch (JsonProcessingException e) {
                    log.error("序列化 sourcesJson 失败", e);
                }
            }

            resourceMapper.insert(res);
            resourceIds.add(res.getId());
        }

        // 保存 AgentRun
        saveAgentRuns(taskId, req.getStudentId(), req.getCourseId(), aiResp.getAgentTrace());

        // 更新 AiTask
        task.setStatus("success");
        task.setFinishedAt(LocalDateTime.now());
        aiTaskMapper.updateById(task);

        Map<String, Object> result = new HashMap<>();
        result.put("taskId", taskId);
        result.put("resourceIds", resourceIds);
        result.put("status", "success");
        return result;
    }

    public List<ResourceResponse> getResourceList(Long studentId) {
        List<Resource> list = resourceMapper.selectList(
                new QueryWrapper<Resource>()
                        .eq("student_id", studentId)
                        .orderByDesc("created_at"));
        return list.stream().map(this::toResourceResponse).collect(Collectors.toList());
    }

    public ResourceResponse getResourceDetail(Long resourceId) {
        Resource res = resourceMapper.selectById(resourceId);
        return res != null ? toResourceResponse(res) : null;
    }

    private ResourceResponse toResourceResponse(Resource res) {
        ResourceResponse resp = new ResourceResponse();
        resp.setId(res.getId());
        resp.setResourceType(res.getResourceType());
        resp.setTitle(res.getTitle());
        resp.setFormat(res.getFormat());
        resp.setQualityScore(res.getQualityScore());
        resp.setStatus(res.getStatus());
        resp.setCreatedAt(res.getCreatedAt());
        resp.setContent(res.getContent());
        return resp;
    }

    private void saveAgentRuns(String taskId, Long studentId, Long courseId, List<AgentTrace> traces) {
        if (traces == null) return;
        for (AgentTrace t : traces) {
            AgentRun run = new AgentRun();
            run.setTaskId(taskId);
            run.setStudentId(studentId);
            run.setCourseId(courseId);
            run.setAgentName(t.getAgentName());
            run.setStatus(t.getStatus());
            run.setInputSummary(t.getInputSummary());
            run.setOutputSummary(t.getOutputSummary());
            run.setModelName(t.getModelName());
            run.setLatencyMs(t.getLatencyMs());
            agentRunMapper.insert(run);
        }
    }
}
