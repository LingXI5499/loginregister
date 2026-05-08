package com.smartblog.service.impl;

import com.smartblog.dto.response.UserInfoResponse;
import com.smartblog.entity.*;
import com.smartblog.exception.BusinessException;
import com.smartblog.mapper.*;
import com.smartblog.service.UserService;
import com.smartblog.util.AuthConstants;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserIdentityMapper identityMapper;

    public UserServiceImpl(UserMapper u, UserIdentityMapper i) {
        userMapper = u;
        identityMapper = i;
    }

    public UserInfoResponse getCurrentUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        UserIdentity username = identityMapper.selectByUserIdAndType(userId, AuthConstants.IDENTITY_USERNAME);
        UserIdentity email = identityMapper.selectByUserIdAndType(userId, AuthConstants.IDENTITY_EMAIL);

        return new UserInfoResponse(
                user.getId(),
                username == null ? null : username.getIdentityValue(),
                email == null ? null : email.getIdentityValue(),
                email == null ? 0 : email.getVerified(),
                user.getNickname(),
                user.getStatus()
        );
    }
}
