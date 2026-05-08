package com.smartblog.dto.request;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank(message = "用户名不能为空") @Size(min = 3, max = 20, message = "用户名长度必须在 3~20 位之间") String username,
        @NotBlank(message = "密码不能为空") @Size(min = 6, max = 64, message = "密码长度必须在 6~64 位之间") String password,
        @NotBlank(message = "邮箱不能为空") @Email(message = "邮箱格式不正确") String email,
        @NotBlank(message = "邮箱验证码不能为空") String emailCode, String nickname) {
}
