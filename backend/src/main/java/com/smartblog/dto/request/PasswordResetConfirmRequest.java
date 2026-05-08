package com.smartblog.dto.request;

import jakarta.validation.constraints.*;

public record PasswordResetConfirmRequest(
        @NotBlank(message = "邮箱不能为空") @Email(message = "邮箱格式不正确") String email,
        @NotBlank(message = "验证码不能为空") String code,
        @NotBlank(message = "新密码不能为空") @Size(min = 6, max = 64, message = "新密码长度必须在 6~64 位之间") String newPassword) {
}
