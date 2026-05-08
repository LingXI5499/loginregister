package com.smartblog.dto.request; import jakarta.validation.constraints.*; public record EmailRequest(@NotBlank(message="邮箱不能为空") @Email(message="邮箱格式不正确") String email){}
