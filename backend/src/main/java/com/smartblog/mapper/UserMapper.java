package com.smartblog.mapper;

import com.smartblog.entity.User;
import org.apache.ibatis.annotations.*;

public interface UserMapper {

    @Select("""
            SELECT
                id,
                nickname,
                avatar_url,
                status,
                create_time,
                update_time
            FROM users
            WHERE id = #{id}
            LIMIT 1
            """)
    User selectById(@Param("id") Long id);

    @Insert("""
            INSERT INTO users(
                nickname,
                avatar_url,
                status
            )
            VALUES(
                #{nickname},
                #{avatarUrl},
                #{status}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertUser(User user);

    @Update("""
            UPDATE users
            SET status = #{status}
            WHERE id = #{userId}
            """)
    int updateStatus(
            @Param("userId") Long userId,
            @Param("status") Integer status
    );

    @Update("""
            UPDATE users
            SET status = #{newStatus}
            WHERE id = #{userId}
              AND status = #{oldStatus}
            """)
    int updateStatusIfCurrent(
            @Param("userId") Long userId,
            @Param("oldStatus") Integer oldStatus,
            @Param("newStatus") Integer newStatus
    );

}
