package com.smartblog.mapper;

import com.smartblog.entity.AuthSession;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;
import java.util.List;

public interface AuthSessionMapper {
    @Insert("""
            INSERT INTO auth_sessions(session_id,user_id,device_id,access_token_jti,refresh_token_hash,ip,user_agent,device_name,status,expire_time)
            VALUES(#{sessionId},#{userId},#{deviceId},#{accessTokenJti},#{refreshTokenHash},#{ip},#{userAgent},#{deviceName},#{status},#{expireTime})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AuthSession s);

    @Select("""
            SELECT id,session_id,user_id,device_id,access_token_jti,refresh_token_hash,ip,user_agent,device_name,status,expire_time,revoked_time,create_time,update_time
            FROM auth_sessions WHERE session_id = #{sessionId} LIMIT 1
            """)
    AuthSession selectBySessionId(@Param("sessionId") String sessionId);

    @Update("""
            UPDATE auth_sessions SET access_token_jti = #{jti}, update_time = NOW()
            WHERE session_id = #{sessionId} AND status = 1
            """)
    int updateAccessTokenJti(@Param("sessionId") String sessionId, @Param("jti") String jti);

    @Update("""
            UPDATE auth_sessions
            SET access_token_jti = #{jti}, refresh_token_hash = #{refreshTokenHash}, expire_time = #{expireTime}, update_time = NOW()
            WHERE session_id = #{sessionId} AND status = 1
            """)
    int rotateRefreshToken(@Param("sessionId") String sessionId, @Param("jti") String jti, @Param("refreshTokenHash") String refreshTokenHash, @Param("expireTime") LocalDateTime expireTime);

    @Update("""
            UPDATE auth_sessions SET status = 0, revoked_time = NOW(), update_time = NOW()
            WHERE session_id = #{sessionId} AND status = 1
            """)
    int revokeBySessionId(@Param("sessionId") String sessionId);

    @Update("""
            UPDATE auth_sessions SET status = 0, revoked_time = NOW(), update_time = NOW()
            WHERE user_id = #{userId} AND status = 1
            """)
    int revokeAllByUserId(@Param("userId") Long userId);

    @Update("""
            UPDATE auth_sessions SET status = 2, update_time = NOW()
            WHERE status = 1 AND expire_time <= #{now}
            """)
    int expireOutdated(@Param("now") LocalDateTime now);

    @Delete("""
            DELETE FROM auth_sessions WHERE update_time < #{before} AND status IN (0, 2)
            """)
    int deleteHistoryBefore(@Param("before") LocalDateTime before);

    @Select("""
            SELECT id,session_id,user_id,device_id,access_token_jti,refresh_token_hash,ip,user_agent,device_name,status,expire_time,revoked_time,create_time,update_time
            FROM auth_sessions
            WHERE user_id = #{userId} AND status = 1 AND expire_time > NOW()
            ORDER BY create_time DESC
            """)
    List<AuthSession> selectActiveByUserId(@Param("userId") Long userId);
}
