package com.smartblog.service.impl;

import com.smartblog.dto.request.PasswordChangeRequest;
import com.smartblog.dto.request.PasswordResetConfirmRequest;
import com.smartblog.dto.request.PasswordResetRequest;
import com.smartblog.dto.response.EmailCodeResponse;
import com.smartblog.entity.User;
import com.smartblog.entity.UserCredential;
import com.smartblog.entity.UserIdentity;
import com.smartblog.exception.BusinessException;
import com.smartblog.mapper.AuthSessionMapper;
import com.smartblog.mapper.UserCredentialMapper;
import com.smartblog.mapper.UserIdentityMapper;
import com.smartblog.mapper.UserMapper;
import com.smartblog.service.EmailCodeService;
import com.smartblog.service.PasswordService;
import com.smartblog.service.SecurityEventService;
import com.smartblog.util.AuthConstants;
import com.smartblog.util.NormalizeUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordServiceImpl implements PasswordService {
    private final UserIdentityMapper identityMapper;
    private final UserMapper userMapper;
    private final UserCredentialMapper credentialMapper;
    private final AuthSessionMapper sessionMapper;
    private final EmailCodeService codeService;
    private final PasswordEncoder passwordEncoder;
    private final SecurityEventService eventService;

    public PasswordServiceImpl(UserIdentityMapper identityMapper, UserMapper userMapper, UserCredentialMapper credentialMapper, AuthSessionMapper sessionMapper, EmailCodeService codeService, PasswordEncoder passwordEncoder, SecurityEventService eventService) {
        this.identityMapper = identityMapper;
        this.userMapper = userMapper;
        this.credentialMapper = credentialMapper;
        this.sessionMapper = sessionMapper;
        this.codeService = codeService;
        this.passwordEncoder = passwordEncoder;
        this.eventService = eventService;
    }

    @Override
    public EmailCodeResponse requestReset(PasswordResetRequest request, HttpServletRequest servletRequest) {
        String email = NormalizeUtil.normalizeEmail(request.email());
        UserIdentity identity = identityMapper.selectByTypeAndValue(AuthConstants.IDENTITY_EMAIL, email);
        if (identity == null) return new EmailCodeResponse("如果该邮箱已注册，我们已发送重置密码验证码", null);
        User user = userMapper.selectById(identity.getUserId());
        if (user == null) return new EmailCodeResponse("如果该邮箱已注册，我们已发送重置密码验证码", null);
        if (user.getStatus() != null && user.getStatus() == AuthConstants.USER_PENDING_DELETION) return new EmailCodeResponse("如果该邮箱已注册，我们已发送重置密码验证码", null);
        codeService.sendCode(AuthConstants.SCENE_RESET_PASSWORD, email, servletRequest);
        return new EmailCodeResponse("如果该邮箱已注册，我们已发送重置密码验证码", null);
    }

    @Override
    @Transactional
    public void confirmReset(PasswordResetConfirmRequest request, HttpServletRequest servletRequest) {
        String email = NormalizeUtil.normalizeEmail(request.email());
        codeService.verifyCode(AuthConstants.SCENE_RESET_PASSWORD, email, request.code());
        UserIdentity identity = identityMapper.selectByTypeAndValue(AuthConstants.IDENTITY_EMAIL, email);
        if (identity == null) throw new BusinessException("验证码错误或已过期");
        User user = userMapper.selectById(identity.getUserId());
        if (user == null) throw new BusinessException("验证码错误或已过期");
        if (user.getStatus() != null && user.getStatus() == AuthConstants.USER_PENDING_DELETION) throw new BusinessException("该账号正在注销中，无法重置密码");
        credentialMapper.updatePasswordByUserId(identity.getUserId(), passwordEncoder.encode(request.newPassword()));
        sessionMapper.revokeAllByUserId(identity.getUserId());
        eventService.log(identity.getUserId(), AuthConstants.EVENT_PASSWORD_RESET, AuthConstants.RESULT_SUCCESS, servletRequest, email);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, PasswordChangeRequest request, HttpServletRequest servletRequest) {
        UserCredential credential = credentialMapper.selectActivePasswordByUserId(userId);
        if (credential == null) throw new BusinessException("未设置密码，无法修改");
        if (!passwordEncoder.matches(request.oldPassword(), credential.getSecretHash())) {
            eventService.log(userId, AuthConstants.EVENT_PASSWORD_CHANGE, AuthConstants.RESULT_FAIL, servletRequest, "旧密码错误");
            throw new BusinessException("旧密码错误");
        }
        credentialMapper.updatePasswordByUserId(userId, passwordEncoder.encode(request.newPassword()));
        sessionMapper.revokeAllByUserId(userId);
        eventService.log(userId, AuthConstants.EVENT_PASSWORD_CHANGE, AuthConstants.RESULT_SUCCESS, servletRequest, "密码修改成功，已撤销全部会话");
    }
}
