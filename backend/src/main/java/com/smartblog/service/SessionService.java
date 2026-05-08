package com.smartblog.service;

import com.smartblog.dto.request.RefreshTokenRequest;
import com.smartblog.dto.response.LoginResponse;
import com.smartblog.dto.response.SessionResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface SessionService {

    LoginResponse createSession(Long userId, String username, String deviceName, HttpServletRequest request);

    LoginResponse refresh(RefreshTokenRequest request);

    void logoutCurrent(HttpServletRequest request);

    void logoutAll(Long userId, HttpServletRequest request);

    void revokeSession(Long userId, String sessionId);

    List<SessionResponse> listSessions(Long userId, String currentSessionId);
}
