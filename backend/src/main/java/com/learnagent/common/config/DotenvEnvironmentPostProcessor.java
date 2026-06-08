package com.learnagent.common.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * 启动时将 .env 文件加载到 Spring Environment 中。
 * 替代 dotenv-java，解决 Spring ${} 占位符无法读取 System.setProperty 的问题。
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
                                        SpringApplication application) {
        File envFile = new File(".env");
        if (!envFile.exists()) {
            return;
        }

        Map<String, Object> envMap = new HashMap<>();
        try {
            List<String> lines = Files.readAllLines(Paths.get(".env"));
            for (String line : lines) {
                line = line.trim();
                // 跳过注释和空行
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eqIdx = line.indexOf('=');
                if (eqIdx < 0) continue;
                String key = line.substring(0, eqIdx).trim();
                String value = line.substring(eqIdx + 1).trim();
                if (!value.isEmpty()) {
                    envMap.put(key, value);
                }
            }
        } catch (IOException e) {
            System.out.println("读取 .env 文件失败: " + e.getMessage());
            return;
        }

        if (!envMap.isEmpty()) {
            // 以最高优先级加载到 Spring Environment
            environment.getPropertySources().addFirst(
                    new MapPropertySource("dotenv", envMap));
        }
    }
}
