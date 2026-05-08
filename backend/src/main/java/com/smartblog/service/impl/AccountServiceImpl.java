package com.smartblog.service.impl;

import com.smartblog.dto.request.AccountDeleteCancelRequest;
import com.smartblog.dto.request.AccountDeleteRequest;
import com.smartblog.dto.request.EmailRequest;
import com.smartblog.dto.response.EmailCodeResponse;
import com.smartblog.entity.AccountDeletionRequest;
import com.smartblog.entity.User;
import com.smartblog.entity.UserIdentity;
import com.smartblog.exception.BusinessException;
import com.smartblog.mapper.AccountDeletionRequestMapper;
import com.smartblog.mapper.AuthSessionMapper;
import com.smartblog.mapper.UserIdentityMapper;
import com.smartblog.mapper.UserMapper;
import com.smartblog.service.AccountService;
import com.smartblog.service.EmailCodeService;
import com.smartblog.service.SecurityEventService;
import com.smartblog.util.AuthConstants;
import com.smartblog.util.NormalizeUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AccountServiceImpl implements AccountService {

    private final UserIdentityMapper identityMapper;
    private final UserMapper userMapper;
    private final AuthSessionMapper sessionMapper;
    private final AccountDeletionRequestMapper deletionMapper;
    private final EmailCodeService codeService;
    private final SecurityEventService eventService;

    @Value("${account.delete.cooldown-days:7}")
    private Integer deleteCooldownDays;

    @Value("${account.delete.finalize-batch-size:100}")
    private Integer finalizeBatchSize;

    public AccountServiceImpl(
            UserIdentityMapper identityMapper,
            UserMapper userMapper,
            AuthSessionMapper sessionMapper,
            AccountDeletionRequestMapper deletionMapper,
            EmailCodeService codeService,
            SecurityEventService eventService
    ) {
        this.identityMapper = identityMapper;
        this.userMapper = userMapper;
        this.sessionMapper = sessionMapper;
        this.deletionMapper = deletionMapper;
        this.codeService = codeService;
        this.eventService = eventService;
    }

    @Override
    public EmailCodeResponse sendDeleteCode(Long userId, HttpServletRequest req) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != AuthConstants.USER_ACTIVE) {
            throw new BusinessException("当前账号不可注销");
        }

        UserIdentity email = identityMapper.selectByUserIdAndType(userId, AuthConstants.IDENTITY_EMAIL);
        if (email == null || email.getVerified() == null || email.getVerified() != 1) {
            throw new BusinessException("当前账号未绑定邮箱");
        }

        return codeService.sendCode(
                AuthConstants.SCENE_DELETE_ACCOUNT,
                email.getNormalizedValue(),
                req
        );
    }

    @Override
    @Transactional
    public void requestDelete(Long userId, AccountDeleteRequest r, HttpServletRequest req) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != AuthConstants.USER_ACTIVE) {
            throw new BusinessException("当前账号不可注销");
        }

        AccountDeletionRequest exists = deletionMapper.selectPendingByUserId(userId);
        if (exists != null) {
            throw new BusinessException("账号已处于注销冷静期内");
        }

        UserIdentity email = identityMapper.selectByUserIdAndType(userId, AuthConstants.IDENTITY_EMAIL);
        if (email == null || email.getVerified() == null || email.getVerified() != 1) {
            throw new BusinessException("当前账号未绑定邮箱");
        }

        codeService.verifyCode(
                AuthConstants.SCENE_DELETE_ACCOUNT,
                email.getNormalizedValue(),
                r.emailCode()
        );

        AccountDeletionRequest adr = new AccountDeletionRequest();
        adr.setUserId(userId);
        adr.setStatus(AuthConstants.DELETION_PENDING);
        adr.setReason(r.reason());
        adr.setCooldownUntil(LocalDateTime.now().plusDays(deleteCooldownDays));

        deletionMapper.insert(adr);

        userMapper.updateStatusIfCurrent(
                userId,
                AuthConstants.USER_ACTIVE,
                AuthConstants.USER_PENDING_DELETION
        );

        sessionMapper.revokeAllByUserId(userId);

        eventService.log(
                userId,
                AuthConstants.EVENT_DELETE_REQUEST,
                AuthConstants.RESULT_SUCCESS,
                req,
                "pending-delete,cooldownDays=" + deleteCooldownDays
        );
    }

    @Override
    public EmailCodeResponse sendCancelDeleteCode(EmailRequest request, HttpServletRequest servletRequest) {
        String email = NormalizeUtil.normalizeEmail(request.email());

        UserIdentity identity = identityMapper.selectByTypeAndValue(
                AuthConstants.IDENTITY_EMAIL,
                email
        );

        if (identity == null) {
            return new EmailCodeResponse("如果账号处于注销冷静期，我们已发送取消注销验证码", null);
        }

        User user = userMapper.selectById(identity.getUserId());
        if (user == null || user.getStatus() == null || user.getStatus() != AuthConstants.USER_PENDING_DELETION) {
            return new EmailCodeResponse("如果账号处于注销冷静期，我们已发送取消注销验证码", null);
        }

        AccountDeletionRequest pending = deletionMapper.selectPendingByUserId(user.getId());
        if (pending == null) {
            return new EmailCodeResponse("如果账号处于注销冷静期，我们已发送取消注销验证码", null);
        }

        codeService.sendCode(
                AuthConstants.SCENE_CANCEL_DELETE_ACCOUNT,
                email,
                servletRequest
        );

        return new EmailCodeResponse("如果账号处于注销冷静期，我们已发送取消注销验证码", null);
    }

    @Override
    @Transactional
    public void cancelDelete(AccountDeleteCancelRequest request, HttpServletRequest servletRequest) {
        String email = NormalizeUtil.normalizeEmail(request.email());

        codeService.verifyCode(
                AuthConstants.SCENE_CANCEL_DELETE_ACCOUNT,
                email,
                request.emailCode()
        );

        UserIdentity identity = identityMapper.selectByTypeAndValue(
                AuthConstants.IDENTITY_EMAIL,
                email
        );

        if (identity == null) {
            throw new BusinessException("验证码错误或已过期");
        }

        User user = userMapper.selectById(identity.getUserId());
        if (user == null || user.getStatus() == null || user.getStatus() != AuthConstants.USER_PENDING_DELETION) {
            throw new BusinessException("账号不处于注销冷静期");
        }

        AccountDeletionRequest pending = deletionMapper.selectPendingByUserId(user.getId());
        if (pending == null) {
            throw new BusinessException("账号不处于注销冷静期");
        }

        int updatedUser = userMapper.updateStatusIfCurrent(
                user.getId(),
                AuthConstants.USER_PENDING_DELETION,
                AuthConstants.USER_ACTIVE
        );

        if (updatedUser != 1) {
            throw new BusinessException("取消注销失败，请稍后重试");
        }

        deletionMapper.cancelPendingByUserId(user.getId());

        eventService.log(
                user.getId(),
                AuthConstants.EVENT_DELETE_CANCEL,
                AuthConstants.RESULT_SUCCESS,
                servletRequest,
                email
        );
    }

    @Override
    @Transactional
    public int finalizeDueDeletionRequests() {
        List<AccountDeletionRequest> dueList = deletionMapper.selectDuePending(
                LocalDateTime.now(),
                finalizeBatchSize
        );

        int count = 0;

        for (AccountDeletionRequest request : dueList) {
            Long userId = request.getUserId();

            User user = userMapper.selectById(userId);
            if (user == null) {
                deletionMapper.markCompleted(request.getId());
                continue;
            }

            if (user.getStatus() == null || user.getStatus() != AuthConstants.USER_PENDING_DELETION) {
                deletionMapper.cancelPendingByUserId(userId);
                continue;
            }

            sessionMapper.revokeAllByUserId(userId);

            identityMapper.anonymizeByUserId(userId);

            userMapper.updateStatusIfCurrent(
                    userId,
                    AuthConstants.USER_PENDING_DELETION,
                    AuthConstants.USER_DELETED
            );

            deletionMapper.markCompleted(request.getId());

            eventService.log(
                    userId,
                    AuthConstants.EVENT_DELETE_FINALIZE,
                    AuthConstants.RESULT_SUCCESS,
                    null,
                    "finalized"
            );

            count++;
        }

        return count;
    }
}
