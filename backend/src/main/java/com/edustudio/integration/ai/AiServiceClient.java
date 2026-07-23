package com.edustudio.integration.ai;

import com.edustudio.common.api.ResultCode;
import com.edustudio.common.exception.BusinessException;
import com.edustudio.integration.ai.dto.AiAgentRunRequest;
import com.edustudio.integration.ai.dto.AiAgentRunResponse;
import com.edustudio.integration.ai.dto.AiChatIntentRequest;
import com.edustudio.integration.ai.dto.AiChatIntentResponse;
import com.edustudio.integration.ai.dto.AiChatRequest;
import com.edustudio.integration.ai.dto.AiChatResponse;
import com.edustudio.integration.ai.dto.AiLearningPathAdjustRequest;
import com.edustudio.integration.ai.dto.AiLearningPathAdjustResponse;
import com.edustudio.integration.ai.dto.AiLearningPathGenerateRequest;
import com.edustudio.integration.ai.dto.AiLearningPathGenerateResponse;
import com.edustudio.integration.ai.dto.AiModelConfigDTO;
import com.edustudio.integration.ai.dto.AiModelTestRequest;
import com.edustudio.integration.ai.dto.AiModelTestResponse;
import com.edustudio.integration.ai.dto.AiProfileAnalyzeRequest;
import com.edustudio.integration.ai.dto.AiProfileAnalyzeResponse;
import com.edustudio.integration.ai.dto.AiQuizAnalyzeRequest;
import com.edustudio.integration.ai.dto.AiQuizAnalyzeResponse;
import com.edustudio.integration.ai.dto.AiQuizGenerateRequest;
import com.edustudio.integration.ai.dto.AiQuizGenerateResponse;
import com.edustudio.integration.ai.dto.AiRagIndexRequest;
import com.edustudio.integration.ai.dto.AiRagIndexResponse;
import com.edustudio.integration.ai.dto.AiRagQaRequest;
import com.edustudio.integration.ai.dto.AiRagQaResponse;
import com.edustudio.integration.ai.dto.AiRagSearchRequest;
import com.edustudio.integration.ai.dto.AiRagSearchResponse;
import com.edustudio.integration.ai.dto.AiReportGenerateRequest;
import com.edustudio.integration.ai.dto.AiReportGenerateResponse;
import com.edustudio.integration.ai.dto.AiResourceGenerateRequest;
import com.edustudio.integration.ai.dto.AiResourceGenerateResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiServiceClient {

    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${eduagent.ai-service.base-url:http://localhost:8000}")
    private String baseUrl;

    public AiModelTestResponse testModel(AiModelTestRequest request) {
        return post("/ai/model/test", request, AiModelTestResponse.class);
    }

    public AiChatResponse chat(AiChatRequest request) {
        return post("/ai/chat", request, AiChatResponse.class);
    }

    public AiChatResponse streamChat(AiChatRequest request, Consumer<String> onDelta) {
        long startedAt = System.nanoTime();
        String model = modelLabel(request);
        try {
            AiChatResponse response = restClientBuilder.build()
                    .post()
                    .uri(baseUrl + "/ai/chat/stream")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .body(request)
                    .exchange((httpRequest, httpResponse) -> {
                        if (!httpResponse.getStatusCode().is2xxSuccessful()) {
                            throw new BusinessException(ResultCode.INTERNAL_ERROR,
                                    "AI 流式接口返回异常状态: " + httpResponse.getStatusCode().value());
                        }
                        AiChatResponse completed = null;
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                                httpResponse.getBody(), StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (!line.startsWith("data:")) {
                                    continue;
                                }
                                String payload = line.substring(5).trim();
                                if (payload.isEmpty()) {
                                    continue;
                                }
                                JsonNode event = objectMapper.readTree(payload);
                                String type = event.path("type").asText();
                                if ("delta".equals(type)) {
                                    String content = event.path("content").asText("");
                                    if (!content.isEmpty()) {
                                        onDelta.accept(content);
                                    }
                                } else if ("done".equals(type) && event.hasNonNull("response")) {
                                    completed = objectMapper.treeToValue(event.get("response"), AiChatResponse.class);
                                } else if ("error".equals(type)) {
                                    throw new BusinessException(ResultCode.INTERNAL_ERROR,
                                            event.path("message").asText("AI 流式生成失败"));
                                }
                            }
                        }
                        if (completed == null) {
                            throw new BusinessException(ResultCode.INTERNAL_ERROR, "AI 流式响应未正常结束");
                        }
                        return completed;
                    });
            log.info("ai_call path=/ai/chat/stream model={} elapsedMs={} success=true", model, elapsedMs(startedAt));
            return response;
        } catch (BusinessException exception) {
            log.warn("ai_call path=/ai/chat/stream model={} elapsedMs={} success=false error={}",
                    model, elapsedMs(startedAt), exception.getMessage());
            throw exception;
        } catch (ResourceAccessException exception) {
            log.warn("ai_call path=/ai/chat/stream model={} elapsedMs={} success=false error={}",
                    model, elapsedMs(startedAt), exception.getMessage());
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "AI 服务不可用，请先启动 Python AI 服务: " + baseUrl);
        } catch (RestClientException exception) {
            log.warn("ai_call path=/ai/chat/stream model={} elapsedMs={} success=false error={}",
                    model, elapsedMs(startedAt), exception.getMessage());
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "AI 流式调用失败: " + exception.getMessage());
        }
    }

    public AiChatIntentResponse detectIntent(AiChatIntentRequest request) {
        return post("/ai/chat/intent", request, AiChatIntentResponse.class);
    }

    public AiAgentRunResponse runAgents(AiAgentRunRequest request) {
        return post("/ai/agents/run", request, AiAgentRunResponse.class);
    }

    public AiResourceGenerateResponse generateResource(AiResourceGenerateRequest request) {
        return post("/ai/resources/generate", request, AiResourceGenerateResponse.class);
    }

    public AiRagIndexResponse indexKnowledge(AiRagIndexRequest request) {
        return post("/ai/rag/index", request, AiRagIndexResponse.class);
    }

    public void deleteKnowledgeIndex(Long fileId) {
        long startedAt = System.nanoTime();
        try {
            restClientBuilder.build()
                    .delete()
                    .uri(baseUrl + "/ai/rag/index/" + fileId)
                    .retrieve()
                    .toBodilessEntity();
            log.info("ai_call path=/ai/rag/index/{} model=none elapsedMs={} success=true", fileId, elapsedMs(startedAt));
        } catch (RestClientException exception) {
            log.warn("ai_call path=/ai/rag/index/{} model=none elapsedMs={} success=false error={}",
                    fileId, elapsedMs(startedAt), exception.getMessage());
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "AI 知识索引清理失败: " + exception.getMessage());
        }
    }

    public AiRagSearchResponse searchKnowledge(AiRagSearchRequest request) {
        return post("/ai/rag/search", request, AiRagSearchResponse.class);
    }

    public AiRagQaResponse answerKnowledge(AiRagQaRequest request) {
        return post("/ai/rag/qa", request, AiRagQaResponse.class);
    }

    public AiLearningPathGenerateResponse generateLearningPath(AiLearningPathGenerateRequest request) {
        return post("/ai/learning-paths/generate", request, AiLearningPathGenerateResponse.class);
    }

    public AiLearningPathAdjustResponse adjustLearningPath(AiLearningPathAdjustRequest request) {
        return post("/ai/learning-paths/adjust", request, AiLearningPathAdjustResponse.class);
    }

    public AiQuizGenerateResponse generateQuiz(AiQuizGenerateRequest request) {
        return post("/ai/quizzes/generate", request, AiQuizGenerateResponse.class);
    }

    public AiQuizAnalyzeResponse analyzeQuiz(AiQuizAnalyzeRequest request) {
        return post("/ai/quizzes/analyze", request, AiQuizAnalyzeResponse.class);
    }

    public AiReportGenerateResponse generateReport(AiReportGenerateRequest request) {
        return post("/ai/reports/generate", request, AiReportGenerateResponse.class);
    }

    public AiProfileAnalyzeResponse analyzeProfile(AiProfileAnalyzeRequest request) {
        return post("/ai/profiles/analyze", request, AiProfileAnalyzeResponse.class);
    }

    private <T> T post(String path, Object request, Class<T> responseType) {
        long startedAt = System.nanoTime();
        String model = modelLabel(request);
        try {
            T response = restClientBuilder.build()
                    .post()
                    .uri(baseUrl + path)
                    .body(request)
                    .retrieve()
                    .body(responseType);
            log.info("ai_call path={} model={} elapsedMs={} success=true", path, model, elapsedMs(startedAt));
            return response;
        } catch (ResourceAccessException exception) {
            log.warn("ai_call path={} model={} elapsedMs={} success=false error={}", path, model, elapsedMs(startedAt), exception.getMessage());
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "AI 服务不可用，请先启动 Python AI 服务: " + baseUrl);
        } catch (RestClientException exception) {
            log.warn("ai_call path={} model={} elapsedMs={} success=false error={}", path, model, elapsedMs(startedAt), exception.getMessage());
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "AI 服务调用失败: " + exception.getMessage());
        }
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private String modelLabel(Object request) {
        try {
            Method method = request.getClass().getMethod("getModelConfig");
            Object value = method.invoke(request);
            if (value instanceof AiModelConfigDTO config) {
                String type = config.getProviderType() == null ? "unknown" : config.getProviderType();
                String name = config.getModelName() == null ? "default" : config.getModelName();
                return type + ":" + name;
            }
        } catch (Exception ignored) {
            return "unknown";
        }
        return "unknown";
    }
}
