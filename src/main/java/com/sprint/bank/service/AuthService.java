package com.sprint.bank.service;

import com.sprint.bank.dto.AuthResponse;
import com.sprint.bank.dto.LoginRequest;
import com.sprint.bank.dto.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
