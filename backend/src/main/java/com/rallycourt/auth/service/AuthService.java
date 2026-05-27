package com.rallycourt.auth.service;

import com.rallycourt.auth.dto.AuthResponse;
import com.rallycourt.auth.dto.LoginRequest;
import com.rallycourt.auth.dto.RegisterRequest;
import com.rallycourt.auth.dto.SessionTokenRequest;

public interface AuthService {

    AuthResponse login(LoginRequest request);

    AuthResponse register(RegisterRequest request);

    AuthResponse refresh(SessionTokenRequest request);
}
