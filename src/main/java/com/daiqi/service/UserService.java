package com.daiqi.service;

import com.daiqi.dto.LoginRequest;
import com.daiqi.dto.LoginResponse;
import com.daiqi.dto.RegisterRequest;

public interface UserService {

    LoginResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    void logout(String token);
}
