package com.smartblog.mapper;

import com.smartblog.entity.AccountDeletionRequest;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

public interface AccountDeletionRequestMapper {

    @Insert("""
            INSERT INTO account_deletion_requests(
                user_id,
                status,
                reason,
                cooldown_until
            )
            VALUES(
                #{userId},
                #{status},
                #{reason},
                #{cooldownUntil}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AccountDeletionRequest r);

    @Select("""
            SELECT
                id,
                user_id,
                status,
                reason,
                request_time,
                cooldown_until,
                finish_time
            FROM account_deletion_requests
            WHERE user_id = #{userId}
              AND status = 1
            ORDER BY request_time DESC
            LIMIT 1
            """)
    AccountDeletionRequest selectPendingByUserId(@Param("userId") Long userId);

    @Update("""
            UPDATE account_deletion_requests
            SET status = 2
            WHERE user_id = #{userId}
              AND status = 1
            """)
    int cancelPendingByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT
                id,
                user_id,
                status,
                reason,
                request_time,
                cooldown_until,
                finish_time
            FROM account_deletion_requests
            WHERE status = 1
              AND cooldown_until IS NOT NULL
              AND cooldown_until <= #{now}
            ORDER BY cooldown_until ASC
            LIMIT #{limit}
            """)
    List<AccountDeletionRequest> selectDuePending(
            @Param("now") LocalDateTime now,
            @Param("limit") Integer limit
    );

    @Update("""
            UPDATE account_deletion_requests
            SET status = 3,
                finish_time = NOW()
            WHERE id = #{id}
              AND status = 1
            """)
    int markCompleted(@Param("id") Long id);
}
