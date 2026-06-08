package com.edustudio.module.auth.vo;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CurrentUserVO {

    private final Long userId;

    private final String username;

    private final String nickname;

    private final String email;

    private final String phone;

    private final String avatarUrl;

    private final String userType;

    private final String status;

    private final List<String> roles;
}
