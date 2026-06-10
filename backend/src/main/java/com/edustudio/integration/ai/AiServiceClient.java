package com.edustudio.integration.ai;

import com.edustudio.common.api.ResultCode;
import com.edustudio.common.exception.BusinessException;
import com.edustudio.integration.ai.dto.AiAgentRunRequest;
import com.edustudio.integration.ai.dto.AiAgentRunResponse;
import com.edustudio.integration.ai.dto.AiChatRequest;
import com.edustudio.integration.ai.dto.AiChatResponse;
import com.edustudio.integration.ai.dto.AiChatIntentRequest;
import com.edustudio.integration.ai.dto.AiChatIntentResponse;
import com.edustudio.integration.ai.dto.AiModelTestRequest;
import com.edustudio.integration.ai.dto.AiModelTestResponse;
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
