package com.edustudio.common.security;

import com.edustudio.common.api.ResultCode;
import com.edustudio.common.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class LoginUserHolder {

    private LoginUserHolder() {
    }

    public static Optional<UserPrincipal> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return Optional.of(userPrincipal);
        }
        return Optional.empty();
    }

    public static UserPrincipal requireCurrentUser() {
        return getCurrentUser()
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "当前用户未登录"));
    }

    public static Long requireCurrentUserId() {
        return requireCurrentUser().getUserId();
    }

    public static void clear() {
        SecurityContextHolder.clearContext();
    }
}
