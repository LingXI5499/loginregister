package com.smartblog.dto.request; import jakarta.validation.constraints.NotBlank; public record AccountDeleteRequest(@NotBlank(message="邮箱验证码不能为空") String emailCode, String reason){}
