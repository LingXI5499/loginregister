package com.smartblog.mapper;

import com.smartblog.entity.UserIdentity;
import org.apache.ibatis.annotations.*;
import java.util.List;

public interface UserIdentityMapper {
    @Insert("""
            INSERT INTO user_identities(user_id,identity_type,identity_value,normalized_value,verified,primary_identity)
            VALUES(#{userId},#{identityType},#{identityValue},#{normalizedValue},#{verified},#{primaryIdentity})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserIdentity i);

    @Select("""
            SELECT id,user_id,identity_type,identity_value,normalized_value,verified,primary_identity,create_time,update_time
            FROM user_identities WHERE identity_type = #{type} AND normalized_value = #{normalizedValue} LIMIT 1
            """)
    UserIdentity selectByTypeAndValue(@Param("type") String type, @Param("normalizedValue") String normalizedValue);

    @Select("""
            SELECT id,user_id,identity_type,identity_value,normalized_value,verified,primary_identity,create_time,update_time
            FROM user_identities WHERE user_id = #{userId} AND identity_type = #{type} LIMIT 1
            """)
    UserIdentity selectByUserIdAndType(@Param("userId") Long userId, @Param("type") String type);

    @Select("""
            SELECT id,user_id,identity_type,identity_value,normalized_value,verified,primary_identity,create_time,update_time
            FROM user_identities WHERE user_id = #{userId}
            """)
    List<UserIdentity> selectByUserId(@Param("userId") Long userId);

    @Update("""
            UPDATE user_identities
            SET identity_value = #{email}, normalized_value = #{normalizedEmail}, verified = 1, update_time = NOW()
            WHERE user_id = #{userId} AND identity_type = 'EMAIL'
            """)
    int updateEmailByUserId(@Param("userId") Long userId, @Param("email") String email, @Param("normalizedEmail") String normalizedEmail);

    @Update("""
            UPDATE user_identities
            SET identity_value = CONCAT('deleted_', user_id, '_', identity_type, '_', id),
                normalized_value = CONCAT('deleted_', user_id, '_', identity_type, '_', id),
                verified = 0,
                primary_identity = 0,
                update_time = NOW()
            WHERE user_id = #{userId}
            """)
    int anonymizeByUserId(@Param("userId") Long userId);
}
