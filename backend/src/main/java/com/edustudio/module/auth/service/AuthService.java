package com.edustudio.module.auth.service;

import com.edustudio.module.auth.dto.LoginRequest;
import com.edustudio.module.auth.dto.RegisterRequest;
import com.edustudio.module.auth.vo.CurrentUserVO;
import com.edustudio.module.auth.vo.LoginResponse;

public interface AuthService {

    CurrentUserVO register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    CurrentUserVO currentUser();

    void logout(String token);
}