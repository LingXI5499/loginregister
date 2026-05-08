package com.smartblog.service;

import com.smartblog.dto.response.EmailCodeResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface EmailCodeService {

    EmailCodeResponse sendCode(String scene, String email, HttpServletRequest request);

    void verifyCode(String scene, String email, String code);
}
