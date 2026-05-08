package com.smartblog.controller;

import com.smartblog.common.ApiResponse;
import com.smartblog.dto.request.EmailChangeCodeRequest;
import com.smartblog.dto.request.EmailChangeConfirmRequest;
import com.smartblog.dto.request.UpdateProfileRequest;
import com.smartblog.dto.response.EmailCodeResponse;
import com.smartblog.dto.response.UserInfoResponse;
import com.smartblog.service.UserService;
import com.smartblog.util.UserContext;
import com.smartblog.vo.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserInfoResponse> me() {
        CurrentUser cu = UserContext.get();
        if (cu == null) return ApiResponse.fail(401, "未登录，请先登录");
        return ApiResponse.success(userService.getCurrentUserInfo(cu.userId()));
    }

    @PutMapping("/profile")
    public ApiResponse<Void> updateProfile(@Valid @RequestBody UpdateProfileRequest request, HttpServletRequest servletRequest) {
        CurrentUser cu = UserContext.get();
        userService.updateProfile(cu.userId(), request, servletRequest);
        return ApiResponse.success("资料修改成功", null);
    }

    @PostMapping("/email/change/code/send")
    public ApiResponse<EmailCodeResponse> sendChangeEmailCode(@Valid @RequestBody EmailChangeCodeRequest request, HttpServletRequest servletRequest) {
        CurrentUser cu = UserContext.get();
        return ApiResponse.success(userService.sendChangeEmailCode(cu.userId(), request, servletRequest));
    }

    @PostMapping("/email/change/confirm")
    public ApiResponse<Void> confirmChangeEmail(@Valid @RequestBody EmailChangeConfirmRequest request, HttpServletRequest servletRequest) {
        CurrentUser cu = UserContext.get();
        userService.confirmChangeEmail(cu.userId(), request, servletRequest);
        return ApiResponse.success("邮箱换绑成功，请重新登录", null);
    }
}
