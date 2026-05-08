package com.smartblog.task;

import com.smartblog.mapper.AuthSessionMapper;
import com.smartblog.mapper.VerificationChallengeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Component
public class AuthDataCleanupTask {
    private static final Logger log = LoggerFactory.getLogger(AuthDataCleanupTask.class);
    private final VerificationChallengeMapper verificationChallengeMapper;
    private final AuthSessionMapper authSessionMapper;

    @Value("${auth.cleanup.verification-retention-days:7}")
    private Integer verificationRetentionDays;

    @Value("${auth.cleanup.session-retention-days:30}")
    private Integer sessionRetentionDays;

    public AuthDataCleanupTask(VerificationChallengeMapper verificationChallengeMapper, AuthSessionMapper authSessionMapper) {
        this.verificationChallengeMapper = verificationChallengeMapper;
        this.authSessionMapper = authSessionMapper;
    }

    @Transactional
    @Scheduled(fixedDelayString = "${auth.cleanup.fixed-delay-ms:600000}")
    public void cleanup() {
        LocalDateTime now = LocalDateTime.now();
        int expiredCodes = verificationChallengeMapper.expireOutdated(now);
        int expiredSessions = authSessionMapper.expireOutdated(now);
        int deletedCodes = verificationChallengeMapper.deleteHistoryBefore(now.minusDays(verificationRetentionDays));
        int deletedSessions = authSessionMapper.deleteHistoryBefore(now.minusDays(sessionRetentionDays));
        if (expiredCodes > 0 || expiredSessions > 0 || deletedCodes > 0 || deletedSessions > 0) {
            log.info("auth cleanup finished, expiredCodes={}, expiredSessions={}, deletedCodes={}, deletedSessions={}", expiredCodes, expiredSessions, deletedCodes, deletedSessions);
        }
    }
}
