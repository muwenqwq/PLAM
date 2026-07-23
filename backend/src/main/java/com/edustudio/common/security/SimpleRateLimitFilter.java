package com.edustudio.common.security;

import com.edustudio.common.api.Result;
import com.edustudio.common.api.ResultCode;
import com.edustudio.common.utils.JsonUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SimpleRateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_SECONDS = 60;
    private static final List<Rule> RULES = List.of(
            new Rule("/api/auth/login", 12),
            new Rule("/api/model-providers/*/test", 20),
            new Rule("/api/chat/conversations/*/messages", 40),
            new Rule("/api/chat/conversations/*/messages/stream", 40),
            new Rule("/api/resources/generate", 30),
            new Rule("/api/quizzes/generate", 30),
            new Rule("/api/reports/generate", 20),
            new Rule("/api/agent-tasks", 20)
    );

    private final AntPathMatcher matcher = new AntPathMatcher();
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Rule rule = matchRule(request);
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }
        String key = clientKey(request) + ':' + rule.pattern();
        Bucket bucket = buckets.compute(key, (ignored, existing) -> {
            long now = Instant.now().getEpochSecond();
            if (existing == null || now - existing.windowStartedAt >= WINDOW_SECONDS) {
                return new Bucket(now, 1);
            }
            existing.count++;
            return existing;
        });
        if (bucket.count > rule.limit()) {
            response.setStatus(429);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(JsonUtils.toJson(Result.failure(ResultCode.TOO_MANY_REQUESTS, "操作过于频繁，请稍后再试")));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private Rule matchRule(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return null;
        }
        String path = request.getRequestURI();
        return RULES.stream().filter(rule -> matcher.match(rule.pattern(), path)).findFirst().orElse(null);
    }

    private String clientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private record Rule(String pattern, int limit) {}

    private static class Bucket {
        private final long windowStartedAt;
        private int count;

        private Bucket(long windowStartedAt, int count) {
            this.windowStartedAt = windowStartedAt;
            this.count = count;
        }
    }
}
