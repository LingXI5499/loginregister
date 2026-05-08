package com.smartblog.dto.response; public record LoginResponse(String accessToken, String refreshToken, Long expiresIn, String username){}
