package com.smartblog.mapper;

import com.smartblog.entity.SecurityEvent;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;

@Mapper
public interface SecurityEventMapper {

    @Insert("INSERT INTO security_events(user_id,event_type,event_result,ip,user_agent,detail) VALUES(#{userId},#{eventType},#{eventResult},#{ip},#{userAgent},#{detail})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SecurityEvent e);

    @Select("SELECT COUNT(1) FROM security_events WHERE event_type='LOGIN_FAIL' AND create_time>=#{since} AND (detail=#{account} OR ip=#{ip})")
    int countRecentLoginFailures(@Param("account") String account, @Param("ip") String ip, @Param("since") LocalDateTime since);
}
