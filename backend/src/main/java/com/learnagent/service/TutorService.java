package com.learnagent.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnagent.client.AiServiceClient;
import com.learnagent.client.dto.AgentTrace;
import com.learnagent.client.dto.AiTutorRequest;
import com.learnagent.client.dto.AiTutorResponse;
import com.learnagent.dto.request.TutorAskRequest;
import com.learnagent.dto.response.AgentTraceItem;
import com.learnagent.dto.response.TutorResponse;
import com.learnagent.entity.AgentRun;
import com.learnagent.entity.AiTask;
import com.learnagent.entity.ProfileSnapshot;
import com.learnagent.entity.TutorMessage;
import com.learnagent.mapper.AgentRunMapper;
import com.learnagent.mapper.AiTaskMapper;
import com.learnagent.mapper.ProfileSnapshotMapper;
import com.learnagent.mapper.TutorMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TutorService {

    private final TutorMessageMapper tutorMessageMapper;
    private final ProfileSnapshotMapper profileSnapshotMapper;
    private final AiTaskMapper aiTaskMapper;
    private final AgentRunMapper agentRunMapper;
    private final AiServiceClient aiServiceClient;
    private final ObjectMapper objectMapper;

    public TutorResponse askTutor(TutorAskRequest req) {
        String taskId = "task_" + System.currentTimeMillis();

        // 创建 AiTask
        AiTask task = new AiTask();
        task.setTaskId(taskId);
        task.setStudentId(req.getStudentId());
        task.setCourseId(req.getCourseId());
        task.setTaskType("tutor_ask");
        task.setStatus("created");
        task.setStartedAt(LocalDateTime.now());
        aiTaskMapper.insert(task);

        // 生成会话 ID
        String sessionId = "session_" + req.getStudentId() + "_" + System.currentTimeMillis();

        // 保存用户消息
        TutorMessage userMsg = new TutorMessage();
        userMsg.setSessionId(sessionId);
        userMsg.setTaskId(taskId);
        userMsg.setStudentId(req.getStudentId());
        userMsg.setCourseId(req.getCourseId());
        userMsg.setMessageRole("user");
        userMsg.setContent(req.getQuestion());
        userMsg.setSafetyStatus("passed");
        tutorMessageMapper.insert(userMsg);

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
        AiTutorRequest aiReq = new AiTutorRequest();
        aiReq.setTaskId(taskId);
        aiReq.setStudentId(req.getStudentId());
        aiReq.setCourseId(req.getCourseId());
        aiReq.setQuestion(req.getQuestion());
        aiReq.setProfileJson(profileMap);

        AiTutorResponse aiResp = aiServiceClient.askTutor(aiReq);

        // 保存助手消息
        TutorMessage assistantMsg = new TutorMessage();
        assistantMsg.setSessionId(sessionId);
        assistantMsg.setTaskId(taskId);
        assistantMsg.setStudentId(req.getStudentId());
        assistantMsg.setCourseId(req.getCourseId());
        assistantMsg.setMessageRole("assistant");
        assistantMsg.setContent(aiResp.getAnswer());
        assistantMsg.setSafetyStatus(aiResp.getSafetyStatus());

        if (aiResp.getSources() != null) {
            try {
                assistantMsg.setSourcesJson(objectMapper.writeValueAsString(aiResp.getSources()));
            } catch (JsonProcessingException e) {
                log.error("序列化 sources 失败", e);
            }
        }
        if (aiResp.getSuggestedResourceIds() != null) {
            try {
                assistantMsg.setSuggestedResourceIds(
                        objectMapper.writeValueAsString(aiResp.getSuggestedResourceIds()));
            } catch (JsonProcessingException e) {
                log.error("序列化 suggestedResourceIds 失败", e);
            }
        }
        tutorMessageMapper.insert(assistantMsg);

        // 保存 AgentRun
        saveAgentRuns(taskId, req.getStudentId(), req.getCourseId(), aiResp.getAgentTrace());

        // 更新 AiTask
        task.setStatus("success");
        task.setFinishedAt(LocalDateTime.now());
        aiTaskMapper.updateById(task);

        // 构建返回
        TutorResponse resp = new TutorResponse();
        resp.setAnswer(aiResp.getAnswer());
        resp.setSuggestedResources(aiResp.getSuggestedResourceIds());

        // 映射 sources
        if (aiResp.getSources() != null) {
            List<TutorResponse.SourceItem> sourceItems = aiResp.getSources().stream().map(s -> {
                TutorResponse.SourceItem item = new TutorResponse.SourceItem();
                item.setFile(s.getOrDefault("file", ""));
                item.setSection(s.getOrDefault("section", ""));
                return item;
            }).collect(Collectors.toList());
            resp.setSources(sourceItems);
        } else {
            resp.setSources(Collections.emptyList());
        }

        // 映射 agentTrace
        if (aiResp.getAgentTrace() != null) {
            List<AgentTraceItem> traceItems = aiResp.getAgentTrace().stream().map(t -> {
                AgentTraceItem item = new AgentTraceItem();
                item.setAgentName(t.getAgentName());
                item.setStatus(t.getStatus());
                item.setInputSummary(t.getInputSummary());
                item.setOutputSummary(t.getOutputSummary());
                item.setModelName(t.getModelName());
                item.setLatencyMs(t.getLatencyMs());
                return item;
            }).collect(Collectors.toList());
            resp.setAgentTrace(traceItems);
        } else {
            resp.setAgentTrace(Collections.emptyList());
        }

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
