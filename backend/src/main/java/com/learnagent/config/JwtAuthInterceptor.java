package com.learnagent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnagent.dto.response.ApiResult;
import com.learnagent.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 认证拦截器
 * 校验请求头中的 Bearer Token
 */
@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        // OPTIONS 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 从 Header 获取 Token
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeError(response, 401, "未登录，请提供 Token");
            return false;
        }

        String token = authHeader.substring(7);
        Long studentId = jwtUtil.getStudentId(token);

        if (studentId == null) {
            writeError(response, 401, "Token 无效或已过期");
            return false;
        }

        // 将 studentId 存入 request，后续接口可通过 request 获取
        request.setAttribute("currentStudentId", studentId);
        return true;
    }

    private void writeError(HttpServletResponse response, int code, String message)
            throws Exception {
        response.setStatus(code);
        response.setContentType("application/json;charset=UTF-8");
        ApiResult<?> result = ApiResult.fail(code, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
