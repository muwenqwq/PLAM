package com.edustudio.integration.ai;

import com.edustudio.common.api.ResultCode;
import com.edustudio.common.exception.BusinessException;
import com.edustudio.integration.ai.dto.AiAgentRunRequest;
import com.edustudio.integration.ai.dto.AiAgentRunResponse;
import com.edustudio.integration.ai.dto.AiChatRequest;
import com.edustudio.integration.ai.dto.AiChatResponse;
import com.edustudio.integration.ai.dto.AiChatIntentRequest;
import com.edustudio.integration.ai.dto.AiChatIntentResponse;
import com.edustudio.integration.ai.dto.AiLearningPathAdjustRequest;
import com.edustudio.integration.ai.dto.AiLearningPathAdjustResponse;
import com.edustudio.integration.ai.dto.AiLearningPathGenerateRequest;
import com.edustudio.integration.ai.dto.AiLearningPathGenerateResponse;
import com.edustudio.integration.ai.dto.AiModelTestRequest;
import com.edustudio.integration.ai.dto.AiModelTestResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class AiServiceClient {

    private final RestClient.Builder restClientBuilder;

    @Value("${eduagent.ai-service.base-url:http://localhost:8000}")
    private String baseUrl;

    public AiModelTestResponse testModel(AiModelTestRequest request) {
        return post("/ai/model/test", request, AiModelTestResponse.class);
    }

    public AiChatResponse chat(AiChatRequest request) {
        return post("/ai/chat", request, AiChatResponse.class);
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

    private <T> T post(String path, Object request, Class<T> responseType) {
        try {
            return restClientBuilder.build()
                    .post()
                    .uri(baseUrl + path)
                    .body(request)
                    .retrieve()
                    .body(responseType);
        } catch (ResourceAccessException exception) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "AI 服务不可用，请先启动 Python AI 服务: " + baseUrl);
        } catch (RestClientException exception) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "AI 服务调用失败: " + exception.getMessage());
        }
    }
}
