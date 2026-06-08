package com.learnagent.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Python AI 服务配置
 * 读取 application.yml 中 ai.service.* 配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.service")
public class AiServiceProperties {

    /** Python AI 服务地址 */
    private String url = "http://localhost:8000";

    /** 调用超时（毫秒） */
    private long timeout = 60000;
}
