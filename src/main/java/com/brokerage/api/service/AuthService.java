package com.brokerage.api.service;
import com.brokerage.api.dto.request.LoginRequest;
import com.brokerage.api.dto.response.JwtResponse;

public interface AuthService {

    JwtResponse login(LoginRequest loginRequest);

//    void createAdminUserIfNotExists();
}