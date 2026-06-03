package com.learnagent.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnagent.client.AiServiceClient;
import com.learnagent.client.dto.AgentTrace;
import com.learnagent.client.dto.AiStudyPlanRequest;
import com.learnagent.client.dto.AiStudyPlanResponse;
import com.learnagent.dto.request.StudyPlanGenerateRequest;
import com.learnagent.dto.response.StudyPlanResponse;
import com.learnagent.entity.*;
import com.learnagent.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudyPlanService {

    private final StudyPlanMapper studyPlanMapper;
    private final StudyPlanNodeMapper studyPlanNodeMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final KnowledgeDependencyMapper knowledgeDependencyMapper;
    private final ResourceMapper resourceMapper;
    private final ProfileSnapshotMapper profileSnapshotMapper;
    private final AiTaskMapper aiTaskMapper;
    private final AgentRunMapper agentRunMapper;
    private final AiServiceClient aiServiceClient;
    private final ObjectMapper objectMapper;

    public StudyPlanResponse generateStudyPlan(StudyPlanGenerateRequest req) {
        String taskId = "task_" + System.currentTimeMillis();

        // 创建 AiTask
        AiTask task = new AiTask();
        task.setTaskId(taskId);
        task.setStudentId(req.getStudentId());
        task.setCourseId(req.getCourseId());
        task.setTaskType("study_plan_generate");
        task.setStatus("created");
        task.setStartedAt(LocalDateTime.now());
        aiTaskMapper.insert(task);

        // 查询知识点和依赖
        List<KnowledgePoint> kps = knowledgePointMapper.selectList(
                new QueryWrapper<KnowledgePoint>().eq("course_id", req.getCourseId()));
        List<KnowledgeDependency> deps = knowledgeDependencyMapper.selectList(
                new QueryWrapper<KnowledgeDependency>().eq("course_id", req.getCourseId()));

        // 查询学生资源
        List<Resource> resources = resourceMapper.selectList(
                new QueryWrapper<Resource>().eq("student_id", req.getStudentId()));

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

        // 构建 AiStudyPlanRequest
        AiStudyPlanRequest aiReq = new AiStudyPlanRequest();
        aiReq.setTaskId(taskId);
        aiReq.setStudentId(req.getStudentId());
        aiReq.setCourseId(req.getCourseId());
        aiReq.setProfileJson(profileMap);

        List<AiStudyPlanRequest.KpInfo> kpInfos = kps.stream().map(kp -> {
            AiStudyPlanRequest.KpInfo info = new AiStudyPlanRequest.KpInfo();
            info.setId(kp.getId());
            info.setName(kp.getName());
            info.setDifficulty(kp.getDifficulty());
            return info;
        }).collect(Collectors.toList());
        aiReq.setKnowledgePoints(kpInfos);

        List<AiStudyPlanRequest.DepInfo> depInfos = deps.stream().map(dep -> {
            AiStudyPlanRequest.DepInfo info = new AiStudyPlanRequest.DepInfo();
            info.setFrom(dep.getFromKnowledgePointId());
            info.setTo(dep.getToKnowledgePointId());
            info.setRelation(dep.getRelation());
            return info;
        }).collect(Collectors.toList());
        aiReq.setDependencies(depInfos);

        List<AiStudyPlanRequest.ResInfo> resInfos = resources.stream().map(r -> {
            AiStudyPlanRequest.ResInfo info = new AiStudyPlanRequest.ResInfo();
            info.setId(r.getId());
            info.setType(r.getResourceType());
            info.setKnowledgePointId(r.getKnowledgePointId());
            return info;
        }).collect(Collectors.toList());
        aiReq.setResources(resInfos);

        // 调用 AI 服务
        AiStudyPlanResponse aiResp = aiServiceClient.generateStudyPlan(aiReq);

        // 保存 StudyPlan
        StudyPlan plan = new StudyPlan();
        plan.setStudentId(req.getStudentId());
        plan.setCourseId(req.getCourseId());
        plan.setTaskId(taskId);
        plan.setTitle("学习路径_" + System.currentTimeMillis());
        plan.setStatus("active");
        if (aiResp.getNodes() != null) {
            int total = aiResp.getNodes().stream()
                    .mapToInt(n -> n.getEstimatedMinutes() != null ? n.getEstimatedMinutes() : 0)
                    .sum();
            plan.setTotalEstimatedMinutes(total);
        }
        studyPlanMapper.insert(plan);

        // 保存 StudyPlanNode
        List<StudyPlanResponse.NodeData> nodeDataList = new ArrayList<>();
        for (AiStudyPlanResponse.NodeItem nodeItem : aiResp.getNodes()) {
            StudyPlanNode node = new StudyPlanNode();
            node.setPlanId(plan.getId());
            node.setKnowledgePointId(nodeItem.getKnowledgePointId());
            node.setKnowledgePointName(nodeItem.getKnowledgePoint());
            node.setNodeOrder(nodeItem.getOrder());
            node.setEstimatedMinutes(nodeItem.getEstimatedMinutes());
            node.setReason(nodeItem.getReason());
            node.setCompletionCriteria(nodeItem.getCompletionCriteria());
            node.setStatus("pending");

            // 序列化 recommendedResourceIds
            if (nodeItem.getRecommendedResourceIds() != null) {
                try {
                    node.setRecommendedResourceIds(
                            objectMapper.writeValueAsString(nodeItem.getRecommendedResourceIds()));
                } catch (JsonProcessingException e) {
                    log.error("序列化 recommendedResourceIds 失败", e);
                }
            }

            studyPlanNodeMapper.insert(node);

            // 构建 NodeData
            StudyPlanResponse.NodeData nd = new StudyPlanResponse.NodeData();
            nd.setId(node.getId());
            nd.setOrder(nodeItem.getOrder());
            nd.setKnowledgePoint(nodeItem.getKnowledgePoint());
            nd.setRecommendedResourceIds(nodeItem.getRecommendedResourceIds());
            nd.setEstimatedDuration(nodeItem.getEstimatedMinutes() + " 分钟");
            nd.setReason(nodeItem.getReason());
            nd.setCompletionCriteria(nodeItem.getCompletionCriteria());
            nd.setStatus("pending");
            nodeDataList.add(nd);
        }

        // 保存 AgentRun
        saveAgentRuns(taskId, req.getStudentId(), req.getCourseId(), aiResp.getAgentTrace());

        // 更新 AiTask
        task.setStatus("success");
        task.setFinishedAt(LocalDateTime.now());
        aiTaskMapper.updateById(task);

        // 构建返回
        StudyPlanResponse resp = new StudyPlanResponse();
        resp.setPlanId(plan.getId());
        resp.setStudentId(req.getStudentId());
        resp.setCourseId(req.getCourseId());
        resp.setCreatedAt(plan.getCreatedAt());
        resp.setNodes(nodeDataList);
        return resp;
    }

    public StudyPlanResponse getStudyPlan(Long studentId) {
        StudyPlan plan = studyPlanMapper.selectOne(
                new QueryWrapper<StudyPlan>()
                        .eq("student_id", studentId)
                        .orderByDesc("created_at")
                        .last("LIMIT 1"));
        if (plan == null) {
            return null;
        }

        List<StudyPlanNode> nodes = studyPlanNodeMapper.selectList(
                new QueryWrapper<StudyPlanNode>()
                        .eq("plan_id", plan.getId())
                        .orderByAsc("node_order"));

        List<StudyPlanResponse.NodeData> nodeDataList = nodes.stream().map(node -> {
            StudyPlanResponse.NodeData nd = new StudyPlanResponse.NodeData();
            nd.setId(node.getId());
            nd.setOrder(node.getNodeOrder());
            nd.setKnowledgePoint(node.getKnowledgePointName());
            nd.setEstimatedDuration(node.getEstimatedMinutes() + " 分钟");
            nd.setReason(node.getReason());
            nd.setCompletionCriteria(node.getCompletionCriteria());
            nd.setStatus(node.getStatus());

            // 反序列化 recommendedResourceIds
            if (node.getRecommendedResourceIds() != null) {
                try {
                    List<Long> ids = objectMapper.readValue(node.getRecommendedResourceIds(),
                            new TypeReference<List<Long>>() {});
                    nd.setRecommendedResourceIds(ids);
                } catch (JsonProcessingException e) {
                    log.error("解析 recommendedResourceIds 失败", e);
                    nd.setRecommendedResourceIds(Collections.emptyList());
                }
            }
            return nd;
        }).collect(Collectors.toList());

        StudyPlanResponse resp = new StudyPlanResponse();
        resp.setPlanId(plan.getId());
        resp.setStudentId(plan.getStudentId());
        resp.setCourseId(plan.getCourseId());
        resp.setCreatedAt(plan.getCreatedAt());
        resp.setNodes(nodeDataList);
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
