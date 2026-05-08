package com.smartblog.service;

import com.smartblog.dto.request.AccountDeleteCancelRequest;
import com.smartblog.dto.request.AccountDeleteRequest;
import com.smartblog.dto.request.EmailRequest;
import com.smartblog.dto.response.EmailCodeResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AccountService {

    EmailCodeResponse sendDeleteCode(Long userId, HttpServletRequest request);

    void requestDelete(Long userId, AccountDeleteRequest request, HttpServletRequest servletRequest);

    EmailCodeResponse sendCancelDeleteCode(EmailRequest request, HttpServletRequest servletRequest);

    void cancelDelete(AccountDeleteCancelRequest request, HttpServletRequest servletRequest);

    int finalizeDueDeletionRequests();
}
