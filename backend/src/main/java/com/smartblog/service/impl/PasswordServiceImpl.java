package com.smartblog.service.impl;

import com.smartblog.dto.request.PasswordChangeRequest;
import com.smartblog.dto.request.PasswordResetConfirmRequest;
import com.smartblog.dto.request.PasswordResetRequest;
import com.smartblog.dto.response.EmailCodeResponse;
import com.smartblog.entity.UserCredential;
import com.smartblog.entity.UserIdentity;
import com.smartblog.exception.BusinessException;
import com.smartblog.mapper.AuthSessionMapper;
import com.smartblog.mapper.UserCredentialMapper;
import com.smartblog.mapper.UserIdentityMapper;
import com.smartblog.service.EmailCodeService;
import com.smartblog.service.PasswordService;
import com.smartblog.service.SecurityEventService;
import com.smartblog.util.AuthConstants;
import com.smartblog.util.NormalizeUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordServiceImpl implements PasswordService {

    private final UserIdentityMapper identityMapper;
    private final UserCredentialMapper credentialMapper;
    private final AuthSessionMapper sessionMapper;
    private final PasswordEncoder encoder;
    private final EmailCodeService codeService;
    private final SecurityEventService eventService;

    @Value("${security.email-code.expire-minutes:10}")
    private Integer expireMinutes;

    public PasswordServiceImpl(
            UserIdentityMapper identityMapper,
            UserCredentialMapper credentialMapper,
            AuthSessionMapper sessionMapper,
            PasswordEncoder encoder,
            EmailCodeService codeService,
            SecurityEventService eventService
    ) {
        this.identityMapper = identityMapper;
        this.credentialMapper = credentialMapper;
        this.sessionMapper = sessionMapper;
        this.encoder = encoder;
        this.codeService = codeService;
        this.eventService = eventService;
    }

    @Override
    public EmailCodeResponse requestReset(PasswordResetRequest r, HttpServletRequest req) {
        String email = NormalizeUtil.normalizeEmail(r.email());
        UserIdentity id = identityMapper.selectByTypeAndValue(AuthConstants.IDENTITY_EMAIL, email);

        if (id == null) {
            return new EmailCodeResponse("如果账号存在，我们已发送重置方式", expireMinutes);
        }

        codeService.sendCode(AuthConstants.SCENE_RESET_PASSWORD, email, req);

        return new EmailCodeResponse("如果账号存在，我们已发送重置方式", expireMinutes);
    }

    @Override
    @Transactional
    public void confirmReset(PasswordResetConfirmRequest r, HttpServletRequest req) {
        String email = NormalizeUtil.normalizeEmail(r.email());

        codeService.verifyCode(AuthConstants.SCENE_RESET_PASSWORD, email, r.code());

        UserIdentity id = identityMapper.selectByTypeAndValue(AuthConstants.IDENTITY_EMAIL, email);
        if (id == null) {
            throw new BusinessException("验证码错误或已过期");
        }

        credentialMapper.updatePasswordByUserId(id.getUserId(), encoder.encode(r.newPassword()));
        sessionMapper.revokeAllByUserId(id.getUserId());

        eventService.log(
                id.getUserId(),
                AuthConstants.EVENT_PASSWORD_RESET,
                AuthConstants.RESULT_SUCCESS,
                req,
                email
        );
    }

    @Override
    @Transactional
    public void changePassword(Long userId, PasswordChangeRequest r, HttpServletRequest req) {
        UserCredential c = credentialMapper.selectActivePasswordByUserId(userId);

        if (c == null || !encoder.matches(r.oldPassword(), c.getSecretHash())) {
            throw new BusinessException("旧密码错误");
        }

        credentialMapper.updatePasswordByUserId(userId, encoder.encode(r.newPassword()));
        sessionMapper.revokeAllByUserId(userId);

        eventService.log(
                userId,
                AuthConstants.EVENT_PASSWORD_CHANGE,
                AuthConstants.RESULT_SUCCESS,
                req,
                "change-password"
        );
    }
}