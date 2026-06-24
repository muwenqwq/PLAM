package com.edustudio.module.health.controller;

import com.edustudio.common.api.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final String applicationName;
    private final Environment environment;
    private final DataSource dataSource;

    public HealthController(
            @Value("${spring.application.name:eduagent-studio-backend}") String applicationName,
            Environment environment,
            @Nullable DataSource dataSource
    ) {
        this.applicationName = applicationName;
        this.environment = environment;
        this.dataSource = dataSource;
    }

    @GetMapping
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("applicationName", applicationName);
        data.put("javaVersion", System.getProperty("java.version"));
        data.put("activeProfiles", environment.getActiveProfiles());
        data.put("currentProfile", currentProfile());
        data.put("currentTime", LocalDateTime.now());
        return Result.success(data);
    }

    @GetMapping("/db")
    public Result<Map<String, Object>> dbHealth() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("currentTime", LocalDateTime.now());

        if (dataSource == null) {
            data.put("connected", false);
            data.put("message", "DataSource 未配置");
            return Result.success(data);
        }

        try (Connection connection = dataSource.getConnection()) {
            data.put("connected", connection.isValid(2));
            data.put("databaseProductName", connection.getMetaData().getDatabaseProductName());
            data.put("databaseProductVersion", connection.getMetaData().getDatabaseProductVersion());
            data.put("message", "数据库连接正常");
        } catch (Exception exception) {
            data.put("connected", false);
            data.put("message", exception.getMessage());
        }
        return Result.success(data);
    }

    private String currentProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length > 0) {
            return String.join(",", activeProfiles);
        }
        String[] defaultProfiles = environment.getDefaultProfiles();
        return defaultProfiles.length == 0 ? "default" : String.join(",", defaultProfiles);
    }
}
