package com.smartblog.dto.request;

import jakarta.validation.constraints.*;

public record EmailCodeLoginRequest(@NotBlank(message = "邮箱不能为空") @Email(message = "邮箱格式不正确") String email,
                                    @NotBlank(message = "验证码不能为空") String code, String deviceName) {
}
