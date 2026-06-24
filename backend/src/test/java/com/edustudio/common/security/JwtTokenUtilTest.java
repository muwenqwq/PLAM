package com.edustudio.common.security;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenUtilTest {

    @Test
    void shouldGenerateAndParseToken() {
        JwtTokenUtil tokenUtil = new JwtTokenUtil(
                "eduagent-studio-test-secret-change-in-production",
                60L,
                "eduagent-studio-test"
        );
        UserPrincipal principal = new UserPrincipal(
                3L,
                "demo_student",
                "encoded-password",
                "学生演示账号",
                "active",
                List.of("STUDENT")
        );

        String token = tokenUtil.generateToken(principal);

        assertThat(token).isNotBlank();
        assertThat(tokenUtil.getUserId(token)).isEqualTo(3L);
        assertThat(tokenUtil.getUsername(token)).isEqualTo("demo_student");
        assertThat(tokenUtil.validateToken(token)).isTrue();
    }
}
