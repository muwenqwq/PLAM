package com.learnagent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnagent.client.AiServiceClient;
import com.learnagent.client.dto.AgentTrace;
import com.learnagent.client.dto.AiProfileRequest;
import com.learnagent.client.dto.AiProfileResponse;
import com.learnagent.dto.request.ProfileExtractRequest;
import com.learnagent.dto.response.ProfileResponse;
import com.learnagent.entity.AgentRun;
import com.learnagent.entity.AiTask;
import com.learnagent.entity.ProfileSnapshot;
import com.learnagent.mapper.AgentRunMapper;
import com.learnagent.mapper.AiTaskMapper;
import com.learnagent.mapper.ProfileSnapshotMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileSnapshotMapper profileSnapshotMapper;
    private final AiTaskMapper aiTaskMapper;
    private final AgentRunMapper agentRunMapper;
    private final AiServiceClient aiServiceClient;
    private final ObjectMapper objectMapper;

    public ProfileResponse extractProfile(ProfileExtractRequest req) {
        String taskId = "task_" + System.currentTimeMillis();

        // 创建 AiTask
        AiTask task = new AiTask();
        task.setTaskId(taskId);
        task.setStudentId(req.getStudentId());
        task.setCourseId(req.getCourseId());
        task.setTaskType("profile_extract");
        task.setStatus("created");
        task.setStartedAt(LocalDateTime.now());
        aiTaskMapper.insert(task);

        // 构建请求，调用 AI 服务
        AiProfileRequest aiReq = new AiProfileRequest();
        aiReq.setTaskId(taskId);
        aiReq.setStudentMessage(req.getStudentMessage());
        aiReq.setCourseContext(Collections.emptyList());

        AiProfileResponse aiResp = aiServiceClient.extractProfile(aiReq);

        // 序列化 profileJson
        String profileJsonStr;
        try {
            profileJsonStr = objectMapper.writeValueAsString(aiResp.getProfileJson());
        } catch (JsonProcessingException e) {
            log.error("序列化 profileJson 失败", e);
            profileJsonStr = "{}";
        }

        // 保存 ProfileSnapshot
        ProfileSnapshot snapshot = new ProfileSnapshot();
        snapshot.setStudentId(req.getStudentId());
        snapshot.setCourseId(req.getCourseId());
        snapshot.setTaskId(taskId);
        snapshot.setProfileJson(profileJsonStr);
        snapshot.setSource("conversation");
        snapshot.setVersionNo(1);
        profileSnapshotMapper.insert(snapshot);

        // 保存 AgentRun
        saveAgentRuns(taskId, req.getStudentId(), req.getCourseId(), aiResp.getAgentTrace());

        // 更新 AiTask 状态
        task.setStatus("success");
        task.setFinishedAt(LocalDateTime.now());
        aiTaskMapper.updateById(task);

        // 构建返回
        return buildProfileResponse(snapshot);
    }

    public ProfileResponse getLatestProfile(Long studentId) {
        ProfileSnapshot ps = profileSnapshotMapper.selectOne(
                new QueryWrapper<ProfileSnapshot>()
                        .eq("student_id", studentId)
                        .orderByDesc("created_at")
                        .last("LIMIT 1"));
        if (ps == null) {
            return null;
        }
        return buildProfileResponse(ps);
    }

    public List<ProfileResponse> getProfileHistory(Long studentId) {
        List<ProfileSnapshot> list = profileSnapshotMapper.selectList(
                new QueryWrapper<ProfileSnapshot>()
                        .eq("student_id", studentId)
                        .orderByDesc("created_at"));
        return list.stream()
                .map(this::buildProfileResponse)
                .collect(Collectors.toList());
    }

    private ProfileResponse buildProfileResponse(ProfileSnapshot ps) {
        ProfileResponse resp = new ProfileResponse();
        resp.setTaskId(ps.getTaskId());
        resp.setProfileSnapshotId(ps.getId());
        resp.setCreatedAt(ps.getCreatedAt());
        try {
            ProfileResponse.ProfileData data = objectMapper.readValue(
                    ps.getProfileJson(), ProfileResponse.ProfileData.class);
            resp.setProfile(data);
        } catch (JsonProcessingException e) {
            log.error("解析 profileJson 失败, snapshotId={}", ps.getId(), e);
            resp.setProfile(new ProfileResponse.ProfileData());
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
