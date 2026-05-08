package com.smartblog.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EmailChangeConfirmRequest(
        @NotBlank(message = "新邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        String newEmail,

        @NotBlank(message = "验证码不能为空")
        @Pattern(regexp = "^\\d{6}$", message = "验证码必须是6位数字")
        String emailCode,

        @NotBlank(message = "当前密码不能为空")
        String currentPassword
) {
}
