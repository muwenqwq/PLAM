package com.learnagent.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnagent.client.AiServiceClient;
import com.learnagent.client.dto.AgentTrace;
import com.learnagent.client.dto.AiAssessmentRequest;
import com.learnagent.client.dto.AiAssessmentResponse;
import com.learnagent.dto.request.AssessmentSubmitRequest;
import com.learnagent.entity.*;
import com.learnagent.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.learnagent.dto.response.AnalyticsResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssessmentService {

    private final AssessmentResultMapper assessmentResultMapper;
    private final ProfileSnapshotMapper profileSnapshotMapper;
    private final AiTaskMapper aiTaskMapper;
    private final AgentRunMapper agentRunMapper;
    private final AiServiceClient aiServiceClient;
    private final ObjectMapper objectMapper;

    public Map<String, Object> submitAssessment(AssessmentSubmitRequest req) {
        String taskId = "task_" + System.currentTimeMillis();

        // 创建 AiTask
        AiTask task = new AiTask();
        task.setTaskId(taskId);
        task.setStudentId(req.getStudentId());
        task.setCourseId(req.getCourseId());
        task.setTaskType("assessment_submit");
        task.setStatus("created");
        task.setStartedAt(LocalDateTime.now());
        aiTaskMapper.insert(task);

        // 查询最新 profile，获取 currentMastery
        ProfileSnapshot ps = profileSnapshotMapper.selectOne(
                new QueryWrapper<ProfileSnapshot>()
                        .eq("student_id", req.getStudentId())
                        .orderByDesc("created_at")
                        .last("LIMIT 1"));

        Map<String, Double> currentMastery = new HashMap<>();
        if (ps != null) {
            try {
                Map<String, Object> profileMap = objectMapper.readValue(ps.getProfileJson(),
                        new TypeReference<Map<String, Object>>() {});
                Object masteryObj = profileMap.get("masteryMap");
                if (masteryObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, ?> rawMap = (Map<String, ?>) masteryObj;
                    for (Map.Entry<String, ?> entry : rawMap.entrySet()) {
                        if (entry.getValue() instanceof Number) {
                            currentMastery.put(entry.getKey(), ((Number) entry.getValue()).doubleValue());
                        }
                    }
                }
            } catch (JsonProcessingException e) {
                log.error("解析 profileJson 获取 masteryMap 失败", e);
            }
        }

        // 转换 answers
        List<Map<String, Object>> answers = new ArrayList<>();
        if (req.getAnswers() != null) {
            for (AssessmentSubmitRequest.AnswerItem item : req.getAnswers()) {
                Map<String, Object> answerMap = new HashMap<>();
                answerMap.put("questionId", item.getQuestionId());
                answerMap.put("answer", item.getAnswer());
                answerMap.put("correct", item.getCorrect());
                answers.add(answerMap);
            }
        }

        // 构建请求，调用 AI 服务
        AiAssessmentRequest aiReq = new AiAssessmentRequest();
        aiReq.setTaskId(taskId);
        aiReq.setStudentId(req.getStudentId());
        aiReq.setCourseId(req.getCourseId());
        aiReq.setKnowledgePointId(req.getKnowledgePointId());
        aiReq.setAnswers(answers);
        aiReq.setCurrentMastery(currentMastery);

        AiAssessmentResponse aiResp = aiServiceClient.analyzeAssessment(aiReq);

        // 保存 AssessmentResult
        AssessmentResult result = new AssessmentResult();
        result.setStudentId(req.getStudentId());
        result.setCourseId(req.getCourseId());
        result.setKnowledgePointId(req.getKnowledgePointId());
        result.setResourceId(req.getResourceId());
        result.setTaskId(taskId);
        result.setQuizTitle("练习评估");  // NOT NULL 必须赋值
        result.setScore(BigDecimal.valueOf(aiResp.getScore()));
        result.setTotalScore(BigDecimal.valueOf(aiResp.getTotalScore()));
        result.setSubmittedAt(LocalDateTime.now());

        if (aiResp.getMasteryDelta() != null) {
            try {
                result.setMasteryDeltaJson(objectMapper.writeValueAsString(aiResp.getMasteryDelta()));
            } catch (JsonProcessingException e) {
                log.error("序列化 masteryDelta 失败", e);
            }
        }
        if (aiResp.getDetails() != null) {
            try {
                result.setDetailsJson(objectMapper.writeValueAsString(aiResp.getDetails()));
            } catch (JsonProcessingException e) {
                log.error("序列化 details 失败", e);
            }
        }

        assessmentResultMapper.insert(result);

        // 更新 profile 的 masteryMap（如果返回了 masteryDelta）
        if (aiResp.getMasteryDelta() != null && ps != null) {
            try {
                Map<String, Object> profileMap = objectMapper.readValue(ps.getProfileJson(),
                        new TypeReference<Map<String, Object>>() {});
                @SuppressWarnings("unchecked")
                Map<String, Object> existingMastery = (Map<String, Object>) profileMap.get("masteryMap");
                Map<String, Object> masteryMap = new HashMap<>();
                if (existingMastery != null) {
                    masteryMap.putAll(existingMastery);
                }
                for (Map.Entry<String, Double> entry : aiResp.getMasteryDelta().entrySet()) {
                    double current = 0.0;
                    Object val = masteryMap.get(entry.getKey());
                    if (val instanceof Number) {
                        current = ((Number) val).doubleValue();
                    }
                    masteryMap.put(entry.getKey(), current + entry.getValue());
                }
                profileMap.put("masteryMap", masteryMap);

                ProfileSnapshot newSnapshot = new ProfileSnapshot();
                newSnapshot.setStudentId(req.getStudentId());
                newSnapshot.setCourseId(req.getCourseId());
                newSnapshot.setTaskId(taskId);
                newSnapshot.setProfileJson(objectMapper.writeValueAsString(profileMap));
                newSnapshot.setSource("assessment");
                newSnapshot.setVersionNo((ps.getVersionNo() != null ? ps.getVersionNo() : 0) + 1);
                profileSnapshotMapper.insert(newSnapshot);
            } catch (JsonProcessingException e) {
                log.error("更新 profile masteryMap 失败", e);
            }
        }

        // 保存 AgentRun
        saveAgentRuns(taskId, req.getStudentId(), req.getCourseId(), aiResp.getAgentTrace());

        // 更新 AiTask
        task.setStatus("success");
        task.setFinishedAt(LocalDateTime.now());
        aiTaskMapper.updateById(task);

        // 构建返回
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("taskId", taskId);
        resultMap.put("score", aiResp.getScore());
        resultMap.put("totalScore", aiResp.getTotalScore());
        resultMap.put("masteryUpdate", aiResp.getMasteryDelta());
        return resultMap;
    }

    /**
     * 查询学生学习评估统计数据
     */
    public AnalyticsResponse getAnalytics(Long studentId) {
        AnalyticsResponse resp = new AnalyticsResponse();
        resp.setStudentId(studentId);

        // 查询最新画像，获取 masteryMap 和 weakPoints
        ProfileSnapshot ps = profileSnapshotMapper.selectOne(
                new QueryWrapper<ProfileSnapshot>()
                        .eq("student_id", studentId)
                        .orderByDesc("created_at")
                        .last("LIMIT 1"));

        Map<String, Double> masteryMap = new HashMap<>();
        if (ps != null) {
            try {
                Map<String, Object> profileMap = objectMapper.readValue(ps.getProfileJson(),
                        new TypeReference<Map<String, Object>>() {});
                Object masteryObj = profileMap.get("masteryMap");
                if (masteryObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, ?> rawMap = (Map<String, ?>) masteryObj;
                    for (Map.Entry<String, ?> entry : rawMap.entrySet()) {
                        if (entry.getValue() instanceof Number) {
                            masteryMap.put(entry.getKey(), ((Number) entry.getValue()).doubleValue());
                        }
                    }
                }
            } catch (JsonProcessingException e) {
                log.error("解析 profileJson 获取 masteryMap 失败", e);
            }
        }
        resp.setMasteryMap(masteryMap);

        // 薄弱点：掌握度低于 0.5 的知识点
        List<String> weakPoints = masteryMap.entrySet().stream()
                .filter(e -> e.getValue() < 0.5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        resp.setWeakPoints(weakPoints);

        // 查询测验结果
        List<AssessmentResult> results = assessmentResultMapper.selectList(
                new QueryWrapper<AssessmentResult>()
                        .eq("student_id", studentId)
                        .orderByDesc("submitted_at"));

        resp.setTotalQuizzes(results.size());

        // 平均分
        if (!results.isEmpty()) {
            double avg = results.stream()
                    .filter(r -> r.getScore() != null && r.getTotalScore() != null && r.getTotalScore().doubleValue() > 0)
                    .mapToDouble(r -> r.getScore().doubleValue() / r.getTotalScore().doubleValue() * 100)
                    .average()
                    .orElse(0);
            resp.setAverageScore((int) Math.round(avg));
        } else {
            resp.setAverageScore(0);
        }

        // 最近测验结果（最多 10 条）
        List<AnalyticsResponse.RecentResult> recentResults = new ArrayList<>();
        for (AssessmentResult r : results.stream().limit(10).collect(Collectors.toList())) {
            AnalyticsResponse.RecentResult rr = new AnalyticsResponse.RecentResult();
            rr.setId(r.getId());
            rr.setQuizTitle(r.getQuizTitle());
            rr.setScore(r.getScore() != null ? r.getScore().doubleValue() : 0);
            rr.setTotalScore(r.getTotalScore() != null ? r.getTotalScore().doubleValue() : 0);
            rr.setSubmittedAt(r.getSubmittedAt());

            // 解析 detailsJson
            if (r.getDetailsJson() != null) {
                try {
                    List<Map<String, Object>> rawDetails = objectMapper.readValue(r.getDetailsJson(),
                            new TypeReference<List<Map<String, Object>>>() {});
                    List<AnalyticsResponse.DetailItem> details = rawDetails.stream().map(d -> {
                        AnalyticsResponse.DetailItem item = new AnalyticsResponse.DetailItem();
                        item.setQuestion(d.get("question") != null ? d.get("question").toString() : "");
                        item.setCorrect(d.get("correct") instanceof Boolean ? (Boolean) d.get("correct") : false);
                        return item;
                    }).collect(Collectors.toList());
                    rr.setDetails(details);
                } catch (JsonProcessingException e) {
                    log.error("解析 detailsJson 失败", e);
                    rr.setDetails(Collections.emptyList());
                }
            } else {
                rr.setDetails(Collections.emptyList());
            }

            recentResults.add(rr);
        }
        resp.setRecentResults(recentResults);

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
