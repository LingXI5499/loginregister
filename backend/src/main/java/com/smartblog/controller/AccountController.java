package com.smartblog.controller;

import com.smartblog.common.ApiResponse;
import com.smartblog.dto.request.AccountDeleteCancelRequest;
import com.smartblog.dto.request.AccountDeleteRequest;
import com.smartblog.dto.request.EmailRequest;
import com.smartblog.dto.response.EmailCodeResponse;
import com.smartblog.service.AccountService;
import com.smartblog.util.UserContext;
import com.smartblog.vo.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/delete/code/send")
    public ApiResponse<EmailCodeResponse> sendDeleteCode(HttpServletRequest request) {
        CurrentUser cu = UserContext.get();
        return ApiResponse.success(accountService.sendDeleteCode(cu.userId(), request));
    }

    @PostMapping("/delete/request")
    public ApiResponse<Void> requestDelete(
            @Valid @RequestBody AccountDeleteRequest request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser cu = UserContext.get();
        accountService.requestDelete(cu.userId(), request, servletRequest);
        return ApiResponse.success("账号已进入注销冷静期", null);
    }

    @PostMapping("/delete/cancel/code/send")
    public ApiResponse<EmailCodeResponse> sendCancelDeleteCode(
            @Valid @RequestBody EmailRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(accountService.sendCancelDeleteCode(request, servletRequest));
    }

    @PostMapping("/delete/cancel/confirm")
    public ApiResponse<Void> cancelDelete(
            @Valid @RequestBody AccountDeleteCancelRequest request,
            HttpServletRequest servletRequest
    ) {
        accountService.cancelDelete(request, servletRequest);
        return ApiResponse.success("账号注销已取消，请重新登录", null);
    }
}
