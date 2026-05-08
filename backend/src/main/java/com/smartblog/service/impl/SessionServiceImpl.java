package com.smartblog.service.impl;

import com.smartblog.dto.request.RefreshTokenRequest;
import com.smartblog.dto.response.LoginResponse;
import com.smartblog.dto.response.SessionResponse;
import com.smartblog.entity.AuthSession;
import com.smartblog.entity.User;
import com.smartblog.entity.UserIdentity;
import com.smartblog.exception.BusinessException;
import com.smartblog.mapper.AuthSessionMapper;
import com.smartblog.mapper.UserIdentityMapper;
import com.smartblog.mapper.UserMapper;
import com.smartblog.service.SecurityEventService;
import com.smartblog.service.SessionService;
import com.smartblog.util.AuthConstants;
import com.smartblog.util.JwtUtil;
import com.smartblog.util.RequestUtil;
import com.smartblog.util.UserContext;
import com.smartblog.vo.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class SessionServiceImpl implements SessionService {

    private final AuthSessionMapper sessionMapper;
    private final UserMapper userMapper;
    private final UserIdentityMapper identityMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder encoder;
    private final SecurityEventService eventService;
    private final SecureRandom random = new SecureRandom();

    @Value("${jwt.refresh-expiration-days:7}")
    private Integer refreshDays;

    public SessionServiceImpl(
            AuthSessionMapper sessionMapper,
            UserMapper userMapper,
            UserIdentityMapper identityMapper,
            JwtUtil jwtUtil,
            PasswordEncoder encoder,
            SecurityEventService eventService
    ) {
        this.sessionMapper = sessionMapper;
        this.userMapper = userMapper;
        this.identityMapper = identityMapper;
        this.jwtUtil = jwtUtil;
        this.encoder = encoder;
        this.eventService = eventService;
    }

    @Override
    @Transactional
    public LoginResponse createSession(
            Long userId,
            String username,
            String deviceName,
            HttpServletRequest request
    ) {
        String sessionId = randomId();
        String refreshSecret = randomSecret();
        String refreshToken = sessionId + "." + refreshSecret;
        String accessTokenJti = randomId();

        AuthSession session = new AuthSession();
        session.setSessionId(sessionId);
        session.setUserId(userId);
        session.setDeviceId(
                StringUtils.hasText(request.getHeader("X-Device-Id"))
                        ? request.getHeader("X-Device-Id")
                        : randomId()
        );
        session.setAccessTokenJti(accessTokenJti);
        session.setRefreshTokenHash(encoder.encode(refreshSecret));
        session.setIp(RequestUtil.getClientIp(request));
        session.setUserAgent(RequestUtil.getUserAgent(request));
        session.setDeviceName(RequestUtil.getDeviceName(request, deviceName));
        session.setStatus(AuthConstants.SESSION_ACTIVE);
        session.setExpireTime(LocalDateTime.now().plusDays(refreshDays));

        sessionMapper.insert(session);

        String accessToken = jwtUtil.generateAccessToken(userId, username, sessionId, accessTokenJti);

        return new LoginResponse(
                accessToken,
                refreshToken,
                jwtUtil.getAccessExpirationSeconds(),
                username
        );
    }

    @Override
    @Transactional
    public LoginResponse refresh(RefreshTokenRequest request) {
        String[] parts = request.refreshToken().split("\\.", 2);
        if (parts.length != 2) {
            throw new BusinessException("refresh token 无效");
        }

        String sessionId = parts[0];
        String oldRefreshSecret = parts[1];

        AuthSession session = sessionMapper.selectBySessionId(sessionId);
        if (session == null
                || session.getStatus() == null
                || session.getStatus() != AuthConstants.SESSION_ACTIVE) {
            throw new BusinessException("会话已失效，请重新登录");
        }

        if (session.getExpireTime() == null || session.getExpireTime().isBefore(LocalDateTime.now())) {
            sessionMapper.revokeBySessionId(sessionId);
            throw new BusinessException("会话已过期，请重新登录");
        }

        if (!encoder.matches(oldRefreshSecret, session.getRefreshTokenHash())) {
            sessionMapper.revokeBySessionId(sessionId);
            throw new BusinessException("refresh token 无效");
        }

        User user = userMapper.selectById(session.getUserId());
        if (user == null
                || user.getStatus() == null
                || user.getStatus() != AuthConstants.USER_ACTIVE) {
            sessionMapper.revokeBySessionId(sessionId);
            throw new BusinessException("账号不可用，请重新登录");
        }

        String newAccessTokenJti = randomId();
        String newRefreshSecret = randomSecret();
        String newRefreshToken = sessionId + "." + newRefreshSecret;
        LocalDateTime newExpireTime = LocalDateTime.now().plusDays(refreshDays);

        int updated = sessionMapper.rotateRefreshToken(
                sessionId,
                newAccessTokenJti,
                encoder.encode(newRefreshSecret),
                newExpireTime
        );

        if (updated != 1) {
            throw new BusinessException("会话已失效，请重新登录");
        }

        String username = getUsername(user.getId());
        String newAccessToken = jwtUtil.generateAccessToken(
                user.getId(),
                username,
                sessionId,
                newAccessTokenJti
        );

        return new LoginResponse(
                newAccessToken,
                newRefreshToken,
                jwtUtil.getAccessExpirationSeconds(),
                username
        );
    }

    @Override
    public void logoutCurrent(HttpServletRequest request) {
        CurrentUser cu = UserContext.get();
        if (cu == null) {
            throw new BusinessException("未登录，请先登录");
        }

        sessionMapper.revokeBySessionId(cu.sessionId());

        eventService.log(
                cu.userId(),
                AuthConstants.EVENT_LOGOUT,
                AuthConstants.RESULT_SUCCESS,
                request,
                cu.sessionId()
        );
    }

    @Override
    public void logoutAll(Long userId, HttpServletRequest request) {
        sessionMapper.revokeAllByUserId(userId);

        eventService.log(
                userId,
                AuthConstants.EVENT_LOGOUT,
                AuthConstants.RESULT_SUCCESS,
                request,
                "logout-all"
        );
    }

    @Override
    public void revokeSession(Long userId, String sessionId) {
        AuthSession session = sessionMapper.selectBySessionId(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException("会话不存在");
        }

        sessionMapper.revokeBySessionId(sessionId);
    }

    @Override
    public List<SessionResponse> listSessions(Long userId, String currentSessionId) {
        return sessionMapper.selectActiveByUserId(userId)
                .stream()
                .map(s -> new SessionResponse(
                        s.getSessionId(),
                        s.getDeviceName(),
                        s.getIp(),
                        s.getCreateTime(),
                        s.getExpireTime(),
                        s.getSessionId().equals(currentSessionId)
                ))
                .toList();
    }

    private String getUsername(Long userId) {
        UserIdentity identity = identityMapper.selectByUserIdAndType(
                userId,
                AuthConstants.IDENTITY_USERNAME
        );

        return identity == null ? "user-" + userId : identity.getIdentityValue();
    }

    private String randomId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String randomSecret() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}