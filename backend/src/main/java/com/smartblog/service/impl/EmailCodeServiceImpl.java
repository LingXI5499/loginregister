package com.smartblog.service.impl;

import com.smartblog.dto.response.EmailCodeResponse;
import com.smartblog.entity.VerificationChallenge;
import com.smartblog.exception.BusinessException;
import com.smartblog.mapper.VerificationChallengeMapper;
import com.smartblog.service.EmailCodeService;
import com.smartblog.service.MailService;
import com.smartblog.service.SecurityEventService;
import com.smartblog.util.AuthConstants;
import com.smartblog.util.NormalizeUtil;
import com.smartblog.util.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class EmailCodeServiceImpl implements EmailCodeService {

    private final VerificationChallengeMapper mapper;
    private final SecurityEventService eventService;
    private final PasswordEncoder encoder;
    private final MailService mailService;
    private final SecureRandom random = new SecureRandom();

    @Value("${security.email-code.expire-minutes:10}")
    private Integer expireMinutes;

    @Value("${security.email-code.send-interval-seconds:60}")
    private Integer intervalSeconds;

    @Value("${security.email-code.max-send-per-hour-target:5}")
    private Integer maxTarget;

    @Value("${security.email-code.max-send-per-hour-ip:20}")
    private Integer maxIp;

    @Value("${security.email-code.max-attempts:5}")
    private Integer maxAttempts;

    public EmailCodeServiceImpl(
            VerificationChallengeMapper mapper,
            SecurityEventService eventService,
            PasswordEncoder encoder,
            MailService mailService
    ) {
        this.mapper = mapper;
        this.eventService = eventService;
        this.encoder = encoder;
        this.mailService = mailService;
    }

    @Override
    @Transactional
    public EmailCodeResponse sendCode(String scene, String email, HttpServletRequest request) {
        String target = NormalizeUtil.normalizeEmail(email);
        String ip = RequestUtil.getClientIp(request);
        LocalDateTime now = LocalDateTime.now();

        if (mapper.countRecentByTarget(scene, target, now.minusSeconds(intervalSeconds)) > 0) {
            throw new BusinessException("验证码发送过于频繁，请稍后再试");
        }

        if (mapper.countRecentByTarget(scene, target, now.minusHours(1)) >= maxTarget) {
            throw new BusinessException("该邮箱验证码发送次数过多，请稍后再试");
        }

        if (mapper.countRecentByIp(scene, ip, now.minusHours(1)) >= maxIp) {
            throw new BusinessException("当前网络验证码发送次数过多，请稍后再试");
        }

        String code = String.format("%06d", random.nextInt(1000000));

        mapper.expireActiveBySceneAndTarget(scene, target);

        VerificationChallenge c = new VerificationChallenge();
        c.setScene(scene);
        c.setTarget(target);
        c.setCodeHash(encoder.encode(code));
        c.setExpireTime(now.plusMinutes(expireMinutes));
        c.setSendIp(ip);
        c.setStatus(1);
        mapper.insert(c);

        mailService.sendVerificationCode(target, scene, code, expireMinutes);

        eventService.log(
                null,
                AuthConstants.EVENT_EMAIL_CODE_SEND,
                AuthConstants.RESULT_SUCCESS,
                request,
                scene + ":" + target
        );

        return new EmailCodeResponse("验证码已发送，请查收邮箱", expireMinutes);
    }

    @Override
    public void verifyCode(String scene, String email, String code) {
        String target = NormalizeUtil.normalizeEmail(email);

        VerificationChallenge c = mapper.selectLatestValid(scene, target, LocalDateTime.now());
        if (c == null) {
            throw new BusinessException("验证码错误或已过期");
        }

        if (c.getAttemptCount() != null && c.getAttemptCount() >= maxAttempts) {
            throw new BusinessException("验证码错误次数过多，请重新获取");
        }

        if (!encoder.matches(code, c.getCodeHash())) {
            mapper.increaseAttempt(c.getId());
            throw new BusinessException("验证码错误或已过期");
        }

        mapper.markUsed(c.getId());
    }
}