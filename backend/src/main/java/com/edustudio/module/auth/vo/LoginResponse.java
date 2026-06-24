package com.edustudio.module.auth.vo;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class LoginResponse {

    private final String tokenType;

    private final String accessToken;

    private final LocalDateTime expiresAt;

    private final CurrentUserVO user;
}
