package com.edustudio.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String KEY_PREFIX = "eduagent:jwt:blacklist:";

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final Map<String, LocalDateTime> localBlacklist = new ConcurrentHashMap<>();

    public void blacklist(String token, LocalDateTime expiresAt) {
        if (!StringUtils.hasText(token) || expiresAt == null) {
            return;
        }
        localBlacklist.put(token, expiresAt);
        Duration ttl = Duration.between(LocalDateTime.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }
        try {
            StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set(KEY_PREFIX + token, "1", ttl);
            }
        } catch (Exception ignored) {
            // Redis is optional for local demos; the in-memory map still protects this JVM.
        }
    }

    public boolean isBlacklisted(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        LocalDateTime expiresAt = localBlacklist.get(token);
        if (expiresAt != null) {
            if (expiresAt.isAfter(LocalDateTime.now())) {
                return true;
            }
            localBlacklist.remove(token);
        }
        try {
            StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
            return redisTemplate != null && Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + token));
        } catch (Exception ignored) {
            return false;
        }
    }
}