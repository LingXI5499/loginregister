package com.smartblog.service;

import com.smartblog.dto.request.EmailChangeCodeRequest;
import com.smartblog.dto.request.EmailChangeConfirmRequest;
import com.smartblog.dto.request.UpdateProfileRequest;
import com.smartblog.dto.response.EmailCodeResponse;
import com.smartblog.dto.response.UserInfoResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface UserService {
    UserInfoResponse getCurrentUserInfo(Long userId);
    void updateProfile(Long userId, UpdateProfileRequest request, HttpServletRequest servletRequest);
    EmailCodeResponse sendChangeEmailCode(Long userId, EmailChangeCodeRequest request, HttpServletRequest servletRequest);
    void confirmChangeEmail(Long userId, EmailChangeConfirmRequest request, HttpServletRequest servletRequest);
}
