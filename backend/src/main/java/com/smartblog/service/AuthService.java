package com.smartblog.service;

import com.smartblog.dto.request.EmailCodeLoginRequest;
import com.smartblog.dto.request.LoginRequest;
import com.smartblog.dto.request.RegisterRequest;
import com.smartblog.dto.response.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    void register(RegisterRequest request, HttpServletRequest servletRequest);

    LoginResponse loginByPassword(LoginRequest request, HttpServletRequest servletRequest);

    LoginResponse loginByEmailCode(EmailCodeLoginRequest request, HttpServletRequest servletRequest);
}
