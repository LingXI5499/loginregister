package com.smartblog.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 50, message = "昵称不能超过 50 个字符")
        String nickname,

        @Size(max = 255, message = "头像地址不能超过 255 个字符")
        String avatarUrl
) {
}
