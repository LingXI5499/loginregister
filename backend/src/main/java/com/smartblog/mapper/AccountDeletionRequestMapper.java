package com.smartblog.mapper;
import com.smartblog.entity.AccountDeletionRequest; import org.apache.ibatis.annotations.*;
public interface AccountDeletionRequestMapper { @Insert("INSERT INTO account_deletion_requests(user_id,status,reason,cooldown_until) VALUES(#{userId},#{status},#{reason},#{cooldownUntil})") @Options(useGeneratedKeys=true,keyProperty="id") int insert(AccountDeletionRequest r); }
