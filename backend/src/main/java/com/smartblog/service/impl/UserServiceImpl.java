package com.smartblog.service.impl;

import com.smartblog.dto.request.EmailChangeCodeRequest;
import com.smartblog.dto.request.EmailChangeConfirmRequest;
import com.smartblog.dto.request.UpdateProfileRequest;
import com.smartblog.dto.response.EmailCodeResponse;
import com.smartblog.dto.response.UserInfoResponse;
import com.smartblog.entity.User;
import com.smartblog.entity.UserCredential;
import com.smartblog.entity.UserIdentity;
import com.smartblog.exception.BusinessException;
import com.smartblog.mapper.AuthSessionMapper;
import com.smartblog.mapper.UserCredentialMapper;
import com.smartblog.mapper.UserIdentityMapper;
import com.smartblog.mapper.UserMapper;
import com.smartblog.service.EmailCodeService;
import com.smartblog.service.SecurityEventService;
import com.smartblog.service.UserService;
import com.smartblog.util.AuthConstants;
import com.smartblog.util.NormalizeUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;
    private final UserIdentityMapper identityMapper;
    private final UserCredentialMapper credentialMapper;
    private final AuthSessionMapper sessionMapper;
    private final EmailCodeService codeService;
    private final PasswordEncoder passwordEncoder;
    private final SecurityEventService eventService;

    public UserServiceImpl(UserMapper userMapper, UserIdentityMapper identityMapper, UserCredentialMapper credentialMapper, AuthSessionMapper sessionMapper, EmailCodeService codeService, PasswordEncoder passwordEncoder, SecurityEventService eventService) {
        this.userMapper = userMapper;
        this.identityMapper = identityMapper;
        this.credentialMapper = credentialMapper;
        this.sessionMapper = sessionMapper;
        this.codeService = codeService;
        this.passwordEncoder = passwordEncoder;
        this.eventService = eventService;
    }

    @Override
    public UserInfoResponse getCurrentUserInfo(Long userId) {
        User user = getExistingUser(userId);
        UserIdentity username = identityMapper.selectByUserIdAndType(userId, AuthConstants.IDENTITY_USERNAME);
        UserIdentity email = identityMapper.selectByUserIdAndType(userId, AuthConstants.IDENTITY_EMAIL);
        return new UserInfoResponse(
                user.getId(),
                username == null ? null : username.getIdentityValue(),
                email == null ? null : email.getIdentityValue(),
                email == null ? 0 : email.getVerified(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getStatus()
        );
    }

    @Override
    @Transactional
    public void updateProfile(Long userId, UpdateProfileRequest request, HttpServletRequest servletRequest) {
        User user = getActiveUser(userId);
        String nickname = StringUtils.hasText(request.nickname()) ? request.nickname().trim() : null;
        String avatarUrl = StringUtils.hasText(request.avatarUrl()) ? request.avatarUrl().trim() : null;
        userMapper.updateProfile(user.getId(), nickname, avatarUrl);
        eventService.log(user.getId(), AuthConstants.EVENT_PROFILE_UPDATE, AuthConstants.RESULT_SUCCESS, servletRequest, "profile-updated");
    }

    @Override
    public EmailCodeResponse sendChangeEmailCode(Long userId, EmailChangeCodeRequest request, HttpServletRequest servletRequest) {
        User user = getActiveUser(userId);
        UserIdentity currentEmail = identityMapper.selectByUserIdAndType(user.getId(), AuthConstants.IDENTITY_EMAIL);
        if (currentEmail == null) throw new BusinessException("当前账号未绑定邮箱");

        String newEmail = NormalizeUtil.normalizeEmail(request.newEmail());
        if (newEmail.equals(currentEmail.getNormalizedValue())) throw new BusinessException("新邮箱不能和当前邮箱相同");
        UserIdentity exists = identityMapper.selectByTypeAndValue(AuthConstants.IDENTITY_EMAIL, newEmail);
        if (exists != null) throw new BusinessException("该邮箱已被其他账号绑定");
        return codeService.sendCode(AuthConstants.SCENE_CHANGE_EMAIL, newEmail, servletRequest);
    }

    @Override
    @Transactional
    public void confirmChangeEmail(Long userId, EmailChangeConfirmRequest request, HttpServletRequest servletRequest) {
        User user = getActiveUser(userId);
        UserIdentity currentEmail = identityMapper.selectByUserIdAndType(user.getId(), AuthConstants.IDENTITY_EMAIL);
        if (currentEmail == null) throw new BusinessException("当前账号未绑定邮箱");

        String newEmail = NormalizeUtil.normalizeEmail(request.newEmail());
        if (newEmail.equals(currentEmail.getNormalizedValue())) throw new BusinessException("新邮箱不能和当前邮箱相同");
        UserIdentity exists = identityMapper.selectByTypeAndValue(AuthConstants.IDENTITY_EMAIL, newEmail);
        if (exists != null && !exists.getUserId().equals(user.getId())) throw new BusinessException("该邮箱已被其他账号绑定");

        UserCredential credential = credentialMapper.selectActivePasswordByUserId(user.getId());
        if (credential == null || !passwordEncoder.matches(request.currentPassword(), credential.getSecretHash())) {
            eventService.log(user.getId(), AuthConstants.EVENT_EMAIL_CHANGE, AuthConstants.RESULT_FAIL, servletRequest, "当前密码错误");
            throw new BusinessException("当前密码错误");
        }
        codeService.verifyCode(AuthConstants.SCENE_CHANGE_EMAIL, newEmail, request.emailCode());
        int updated = identityMapper.updateEmailByUserId(user.getId(), newEmail, newEmail);
        if (updated != 1) throw new BusinessException("邮箱换绑失败，请稍后重试");
        sessionMapper.revokeAllByUserId(user.getId());
        eventService.log(user.getId(), AuthConstants.EVENT_EMAIL_CHANGE, AuthConstants.RESULT_SUCCESS, servletRequest, currentEmail.getNormalizedValue() + "->" + newEmail);
    }

    private User getExistingUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        return user;
    }

    private User getActiveUser(Long userId) {
        User user = getExistingUser(userId);
        if (user.getStatus() == null || user.getStatus() != AuthConstants.USER_ACTIVE) throw new BusinessException("账号不可用");
        return user;
    }
}
