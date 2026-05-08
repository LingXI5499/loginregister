package com.smartblog.mapper;

import com.smartblog.entity.VerificationChallenge;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;

public interface VerificationChallengeMapper {

    @Insert("""
            INSERT INTO verification_challenges(
                scene,
                target,
                code_hash,
                expire_time,
                send_ip,
                status
            )
            VALUES(
                #{scene},
                #{target},
                #{codeHash},
                #{expireTime},
                #{sendIp},
                #{status}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(VerificationChallenge c);

    @Select("""
            SELECT COUNT(1)
            FROM verification_challenges
            WHERE scene = #{scene}
              AND target = #{target}
              AND create_time >= #{since}
            """)
    int countRecentByTarget(
            @Param("scene") String scene,
            @Param("target") String target,
            @Param("since") LocalDateTime since
    );

    @Select("""
            SELECT COUNT(1)
            FROM verification_challenges
            WHERE scene = #{scene}
              AND send_ip = #{ip}
              AND create_time >= #{since}
            """)
    int countRecentByIp(
            @Param("scene") String scene,
            @Param("ip") String ip,
            @Param("since") LocalDateTime since
    );

    @Select("""
            SELECT
                id,
                scene,
                target,
                code_hash,
                expire_time,
                used_time,
                attempt_count,
                send_ip,
                status,
                create_time
            FROM verification_challenges
            WHERE scene = #{scene}
              AND target = #{target}
              AND status = 1
              AND used_time IS NULL
              AND expire_time > #{now}
            ORDER BY create_time DESC
            LIMIT 1
            """)
    VerificationChallenge selectLatestValid(
            @Param("scene") String scene,
            @Param("target") String target,
            @Param("now") LocalDateTime now
    );

    @Update("""
            UPDATE verification_challenges
            SET status = 2
            WHERE scene = #{scene}
              AND target = #{target}
              AND status = 1
              AND used_time IS NULL
            """)
    int expireActiveBySceneAndTarget(
            @Param("scene") String scene,
            @Param("target") String target
    );

    @Update("""
            UPDATE verification_challenges
            SET attempt_count = attempt_count + 1
            WHERE id = #{id}
            """)
    int increaseAttempt(@Param("id") Long id);

    @Update("""
            UPDATE verification_challenges
            SET status = 0,
                used_time = NOW()
            WHERE id = #{id}
            """)
    int markUsed(@Param("id") Long id);
}