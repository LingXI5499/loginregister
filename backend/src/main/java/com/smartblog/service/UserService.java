package com.smartblog.service;

import com.smartblog.dto.response.UserInfoResponse;

public interface UserService {

    UserInfoResponse getCurrentUserInfo(Long userId);
}