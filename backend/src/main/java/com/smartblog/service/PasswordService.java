package com.smartblog.service;

import com.smartblog.dto.request.PasswordChangeRequest;
import com.smartblog.dto.request.PasswordResetConfirmRequest;
import com.smartblog.dto.request.PasswordResetRequest;
import com.smartblog.dto.response.EmailCodeResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface PasswordService {

    EmailCodeResponse requestReset(PasswordResetRequest request, HttpServletRequest servletRequest);

    void confirmReset(PasswordResetConfirmRequest request, HttpServletRequest servletRequest);

    void changePassword(Long userId, PasswordChangeRequest request, HttpServletRequest servletRequest);
}
