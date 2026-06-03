package com.learnagent.client;

import com.learnagent.client.dto.*;
import com.learnagent.config.AiServiceProperties;
import com.learnagent.exception.BizException;
import com.learnagent.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiServiceClient {

    private final RestTemplate restTemplate;
    private final AiServiceProperties properties;

    /**
     * 通用 POST 调用 Python AI 服务
     */
    private <T> T post(String path, Object request, Class<T> responseType) {
        String url = properties.getUrl() + path;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> entity = new HttpEntity<>(request, headers);

        try {
            log.info("调用 AI 服务: POST {}", url);
            ResponseEntity<T> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, responseType);
            return response.getBody();
        } catch (ResourceAccessException e) {
            log.error("AI 服务不可用: {}", url, e);
            throw new BizException(ErrorCode.AI_SERVICE_UNAVAILABLE, "AI 服务不可用: " + e.getMessage());
        } catch (Exception e) {
            log.error("AI 服务调用失败: {}", url, e);
            throw new BizException(ErrorCode.INTERNAL_ERROR, "AI 服务调用失败: " + e.getMessage());
        }
    }

    /** 画像抽取 */
    public AiProfileResponse extractProfile(AiProfileRequest request) {
        return post("/ai/profile/extract", request, AiProfileResponse.class);
    }

    /** 资源生成 */
    public AiResourceResponse generateResources(AiResourceRequest request) {
        return post("/ai/resources/generate", request, AiResourceResponse.class);
    }

    /** 学习路径生成 */
    public AiStudyPlanResponse generateStudyPlan(AiStudyPlanRequest request) {
        return post("/ai/study-plan/generate", request, AiStudyPlanResponse.class);
    }

    /** 智能辅导 */
    public AiTutorResponse askTutor(AiTutorRequest request) {
        return post("/ai/tutor/ask", request, AiTutorResponse.class);
    }

    /** 练习分析 */
    public AiAssessmentResponse analyzeAssessment(AiAssessmentRequest request) {
        return post("/ai/assessment/analyze", request, AiAssessmentResponse.class);
    }
}
