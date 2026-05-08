CREATE DATABASE IF NOT EXISTS smartblog_auth
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE smartblog_auth;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS account_deletion_requests;
DROP TABLE IF EXISTS security_events;
DROP TABLE IF EXISTS verification_challenges;
DROP TABLE IF EXISTS auth_sessions;
DROP TABLE IF EXISTS user_credentials;
DROP TABLE IF EXISTS user_identities;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE users (
                       id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
                       nickname VARCHAR(50) DEFAULT NULL COMMENT '昵称',
                       avatar_url VARCHAR(255) DEFAULT NULL COMMENT '头像',
                       status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1正常,0禁用,2待注销,3已注销',
                       create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                       update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户主体表';

CREATE TABLE user_identities (
                                 id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
                                 user_id BIGINT NOT NULL COMMENT '用户ID',
                                 identity_type VARCHAR(20) NOT NULL COMMENT 'USERNAME/EMAIL/PHONE',
                                 identity_value VARCHAR(100) NOT NULL COMMENT '原始标识值',
                                 normalized_value VARCHAR(100) NOT NULL COMMENT '规范化标识值',
                                 verified TINYINT NOT NULL DEFAULT 0 COMMENT '是否已验证:1是,0否',
                                 primary_identity TINYINT NOT NULL DEFAULT 0 COMMENT '是否主标识:1是,0否',
                                 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

                                 UNIQUE KEY uk_identity_type_value (identity_type, normalized_value),
                                 KEY idx_identity_user_id (user_id),

                                 CONSTRAINT fk_user_identities_user
                                     FOREIGN KEY (user_id) REFERENCES users(id)
                                         ON DELETE CASCADE
                                         ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户登录标识表';

CREATE TABLE user_credentials (
                                  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
                                  user_id BIGINT NOT NULL COMMENT '用户ID',
                                  credential_type VARCHAR(20) NOT NULL COMMENT 'PASSWORD/TOTP/PASSKEY',
                                  secret_hash VARCHAR(255) NOT NULL COMMENT '密码哈希或凭据密文',
                                  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1有效,0失效',
                                  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

                                  KEY idx_credential_user_id (user_id),

                                  CONSTRAINT fk_user_credentials_user
                                      FOREIGN KEY (user_id) REFERENCES users(id)
                                          ON DELETE CASCADE
                                          ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户认证凭据表';

CREATE TABLE auth_sessions (
                               id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
                               session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
                               user_id BIGINT NOT NULL COMMENT '用户ID',
                               device_id VARCHAR(64) DEFAULT NULL COMMENT '设备ID',
                               access_token_jti VARCHAR(64) DEFAULT NULL COMMENT '当前有效access token jti',
                               refresh_token_hash VARCHAR(255) NOT NULL COMMENT 'refresh token哈希',
                               ip VARCHAR(64) DEFAULT NULL COMMENT '登录IP',
                               user_agent VARCHAR(500) DEFAULT NULL COMMENT 'User-Agent',
                               device_name VARCHAR(100) DEFAULT NULL COMMENT '设备名称',
                               status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1有效,0已退出,2已过期',
                               expire_time DATETIME NOT NULL COMMENT 'refresh token过期时间',
                               revoked_time DATETIME DEFAULT NULL COMMENT '撤销时间',
                               create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

                               UNIQUE KEY uk_session_id (session_id),
                               KEY idx_session_user_id (user_id),
                               KEY idx_session_status_expire (status, expire_time),

                               CONSTRAINT fk_auth_sessions_user
                                   FOREIGN KEY (user_id) REFERENCES users(id)
                                       ON DELETE CASCADE
                                       ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录会话表';

CREATE TABLE verification_challenges (
                                         id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
                                         scene VARCHAR(40) NOT NULL COMMENT 'REGISTER_EMAIL/LOGIN_EMAIL/RESET_PASSWORD/DELETE_ACCOUNT',
                                         target VARCHAR(100) NOT NULL COMMENT '邮箱或手机号',
                                         code_hash VARCHAR(255) NOT NULL COMMENT '验证码哈希',
                                         expire_time DATETIME NOT NULL COMMENT '过期时间',
                                         used_time DATETIME DEFAULT NULL COMMENT '使用时间',
                                         attempt_count INT NOT NULL DEFAULT 0 COMMENT '错误尝试次数',
                                         send_ip VARCHAR(64) DEFAULT NULL COMMENT '发送IP',
                                         status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1有效,0已使用,2已过期',
                                         create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

                                         KEY idx_challenge_target_scene (target, scene),
                                         KEY idx_challenge_scene_ip_time (scene, send_ip, create_time),
                                         KEY idx_challenge_active_lookup (scene, target, status, used_time, expire_time, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='验证码挑战表';

CREATE TABLE security_events (
                                 id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
                                 user_id BIGINT DEFAULT NULL COMMENT '用户ID，可为空',
                                 event_type VARCHAR(50) NOT NULL COMMENT 'REGISTER/LOGIN_SUCCESS/LOGIN_FAIL/LOGOUT/PASSWORD_CHANGE/PASSWORD_RESET/EMAIL_CODE_SEND/DELETE_REQUEST',
                                 event_result VARCHAR(20) NOT NULL COMMENT 'SUCCESS/FAIL',
                                 ip VARCHAR(64) DEFAULT NULL COMMENT '来源IP',
                                 user_agent VARCHAR(500) DEFAULT NULL COMMENT 'User-Agent',
                                 detail VARCHAR(500) DEFAULT NULL COMMENT '事件详情',
                                 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

                                 KEY idx_event_user_id (user_id),
                                 KEY idx_event_type_time (event_type, create_time),
                                 KEY idx_event_ip_time (ip, create_time),

                                 CONSTRAINT fk_security_events_user
                                     FOREIGN KEY (user_id) REFERENCES users(id)
                                         ON DELETE SET NULL
                                         ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='安全事件日志表';

CREATE TABLE account_deletion_requests (
                                           id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
                                           user_id BIGINT NOT NULL COMMENT '用户ID',
                                           status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1待注销,2已取消,3已完成',
                                           reason VARCHAR(255) DEFAULT NULL COMMENT '注销原因',
                                           request_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
                                           cooldown_until DATETIME DEFAULT NULL COMMENT '冷静期截止时间',
                                           finish_time DATETIME DEFAULT NULL COMMENT '完成时间',

                                           KEY idx_delete_user_id (user_id),

                                           CONSTRAINT fk_account_deletion_requests_user
                                               FOREIGN KEY (user_id) REFERENCES users(id)
                                                   ON DELETE CASCADE
                                                   ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账号注销申请表';




