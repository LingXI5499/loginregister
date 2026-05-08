package com.smartblog.dto.response;

public record EmailCodeResponse(String message, Integer expiresInMinutes) {
}