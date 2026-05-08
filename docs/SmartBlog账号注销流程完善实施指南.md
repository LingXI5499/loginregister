# SmartBlog 账号注销流程完善实施指南

本文档用于在当前项目基础上补齐以下功能：

1. 取消注销
2. 最终注销
3. 标识匿名化
4. 忘记密码对待注销账号的特殊处理

适用范围：后端 Spring Boot + MyBatis 项目。

---

## 一、本轮目标

完成后，注销流程会变成：

```text
用户申请注销
  ↓
users.status = 2 待注销
account_deletion_requests.status = 1 待注销
cooldown_until = 当前时间 + 7天
  ↓
冷静期内：
  - 不能正常登录
  - 不能通过忘记密码恢复账号
  - 可以通过邮箱验证码取消注销
  ↓
冷静期结束：
  - 定时任务执行最终注销
  - user_identities 被匿名化
  - users.status = 3 已注销
  - account_deletion_requests.status = 3 已完成
  ↓
原用户名 / 邮箱释放，可以重新注册
```

---

## 二、数据库说明

本轮不需要新增数据库字段。

需要确认你的 `schema.sql` 中已经包含以下字段：

```sql
users.status

account_deletion_requests.status
account_deletion_requests.cooldown_until
account_deletion_requests.finish_time

user_identities.identity_value
user_identities.normalized_value
user_identities.verified
user_identities.primary_identity
```

如果你已经使用上一版完整 SQL 初始化过数据库，那么本轮不需要改 SQL。

---

## 三、修改 `AuthConstants.java`

文件路径：

```text
backend/src/main/java/com/smartblog/util/AuthConstants.java
```

直接替换为：

```java
package com.smartblog.util;

public class AuthConstants {

    private AuthConstants() {
    }

    public static final String IDENTITY_USERNAME = "USERNAME";
    public static final String IDENTITY_EMAIL = "EMAIL";

    public static final String CREDENTIAL_PASSWORD = "PASSWORD";

    public static final String SCENE_REGISTER_EMAIL = "REGISTER_EMAIL";
    public static final String SCENE_LOGIN_EMAIL = "LOGIN_EMAIL";
    public static final String SCENE_RESET_PASSWORD = "RESET_PASSWORD";
    public static final String SCENE_DELETE_ACCOUNT = "DELETE_ACCOUNT";
    public static final String SCENE_CANCEL_DELETE_ACCOUNT = "CANCEL_DELETE_ACCOUNT";

    public static final String EVENT_REGISTER = "REGISTER";
    public static final String EVENT_LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String EVENT_LOGIN_FAIL = "LOGIN_FAIL";
    public static final String EVENT_LOGOUT = "LOGOUT";
    public static final String EVENT_PASSWORD_CHANGE = "PASSWORD_CHANGE";
    public static final String EVENT_PASSWORD_RESET = "PASSWORD_RESET";
    public static final String EVENT_EMAIL_CODE_SEND = "EMAIL_CODE_SEND";
    public static final String EVENT_DELETE_REQUEST = "DELETE_REQUEST";
    public static final String EVENT_DELETE_CANCEL = "DELETE_CANCEL";
    public static final String EVENT_DELETE_FINALIZE = "DELETE_FINALIZE";

    public static final String RESULT_SUCCESS = "SUCCESS";
    public static final String RESULT_FAIL = "FAIL";

    public static final int USER_DISABLED = 0;
    public static final int USER_ACTIVE = 1;
    public static final int USER_PENDING_DELETION = 2;
    public static final int USER_DELETED = 3;

    public static final int SESSION_REVOKED = 0;
    public static final int SESSION_ACTIVE = 1;
    public static final int SESSION_EXPIRED = 2;

    public static final int DELETION_PENDING = 1;
    public static final int DELETION_CANCELLED = 2;
    public static final int DELETION_COMPLETED = 3;
}
```

---

## 四、新增取消注销请求 DTO

新增文件：

```text
backend/src/main/java/com/smartblog/dto/request/AccountDeleteCancelRequest.java
```

内容：

```java
package com.smartblog.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AccountDeleteCancelRequest(

        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        String email,

        @NotBlank(message = "验证码不能为空")
        @Pattern(regexp = "^\\d{6}$", message = "验证码必须是6位数字")
        String emailCode
) {
}
```

说明：

取消注销接口应允许未登录用户调用，因为待注销账号已经不能正常登录。

---

## 五、替换 `AccountDeletionRequestMapper.java`

文件路径：

```text
backend/src/main/java/com/smartblog/mapper/AccountDeletionRequestMapper.java
```

直接替换为：

```java
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
```

---

## 六、替换 `UserIdentityMapper.java`

文件路径：

```text
backend/src/main/java/com/smartblog/mapper/UserIdentityMapper.java
```

直接替换为：

```java
package com.smartblog.mapper;

import com.smartblog.entity.UserIdentity;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface UserIdentityMapper {

    @Insert("""
            INSERT INTO user_identities(
                user_id,
                identity_type,
                identity_value,
                normalized_value,
                verified,
                primary_identity
            )
            VALUES(
                #{userId},
                #{identityType},
                #{identityValue},
                #{normalizedValue},
                #{verified},
                #{primaryIdentity}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserIdentity i);

    @Select("""
            SELECT
                id,
                user_id,
                identity_type,
                identity_value,
                normalized_value,
                verified,
                primary_identity,
                create_time,
                update_time
            FROM user_identities
            WHERE identity_type = #{type}
              AND normalized_value = #{normalizedValue}
            LIMIT 1
            """)
    UserIdentity selectByTypeAndValue(
            @Param("type") String type,
            @Param("normalizedValue") String normalizedValue
    );

    @Select("""
            SELECT
                id,
                user_id,
                identity_type,
                identity_value,
                normalized_value,
                verified,
                primary_identity,
                create_time,
                update_time
            FROM user_identities
            WHERE user_id = #{userId}
              AND identity_type = #{type}
            LIMIT 1
            """)
    UserIdentity selectByUserIdAndType(
            @Param("userId") Long userId,
            @Param("type") String type
    );

    @Select("""
            SELECT
                id,
                user_id,
                identity_type,
                identity_value,
                normalized_value,
                verified,
                primary_identity,
                create_time,
                update_time
            FROM user_identities
            WHERE user_id = #{userId}
            """)
    List<UserIdentity> selectByUserId(@Param("userId") Long userId);

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
```

匿名化效果示例：

```text
原来：
EMAIL / test@example.com
USERNAME / zhangsan

最终注销后：
EMAIL / deleted_12_EMAIL_22
USERNAME / deleted_12_USERNAME_21
```

这样唯一索引就释放了，原邮箱和用户名可以重新注册。

---

## 七、替换 `UserMapper.java`

文件路径：

```text
backend/src/main/java/com/smartblog/mapper/UserMapper.java
```

直接替换为：

```java
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
```

---

## 八、替换 `AccountService.java`

文件路径：

```text
backend/src/main/java/com/smartblog/service/AccountService.java
```

直接替换为：

```java
package com.smartblog.service;

import com.smartblog.dto.request.AccountDeleteCancelRequest;
import com.smartblog.dto.request.AccountDeleteRequest;
import com.smartblog.dto.request.EmailRequest;
import com.smartblog.dto.response.EmailCodeResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AccountService {

    EmailCodeResponse sendDeleteCode(Long userId, HttpServletRequest request);

    void requestDelete(Long userId, AccountDeleteRequest request, HttpServletRequest servletRequest);

    EmailCodeResponse sendCancelDeleteCode(EmailRequest request, HttpServletRequest servletRequest);

    void cancelDelete(AccountDeleteCancelRequest request, HttpServletRequest servletRequest);

    int finalizeDueDeletionRequests();
}
```

---

## 九、替换 `AccountServiceImpl.java`

文件路径：

```text
backend/src/main/java/com/smartblog/service/impl/AccountServiceImpl.java
```

直接替换为：

```java
package com.smartblog.service.impl;

import com.smartblog.dto.request.AccountDeleteCancelRequest;
import com.smartblog.dto.request.AccountDeleteRequest;
import com.smartblog.dto.request.EmailRequest;
import com.smartblog.dto.response.EmailCodeResponse;
import com.smartblog.entity.AccountDeletionRequest;
import com.smartblog.entity.User;
import com.smartblog.entity.UserIdentity;
import com.smartblog.exception.BusinessException;
import com.smartblog.mapper.AccountDeletionRequestMapper;
import com.smartblog.mapper.AuthSessionMapper;
import com.smartblog.mapper.UserIdentityMapper;
import com.smartblog.mapper.UserMapper;
import com.smartblog.service.AccountService;
import com.smartblog.service.EmailCodeService;
import com.smartblog.service.SecurityEventService;
import com.smartblog.util.AuthConstants;
import com.smartblog.util.NormalizeUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AccountServiceImpl implements AccountService {

    private final UserIdentityMapper identityMapper;
    private final UserMapper userMapper;
    private final AuthSessionMapper sessionMapper;
    private final AccountDeletionRequestMapper deletionMapper;
    private final EmailCodeService codeService;
    private final SecurityEventService eventService;

    @Value("${account.delete.cooldown-days:7}")
    private Integer deleteCooldownDays;

    @Value("${account.delete.finalize-batch-size:100}")
    private Integer finalizeBatchSize;

    public AccountServiceImpl(
            UserIdentityMapper identityMapper,
            UserMapper userMapper,
            AuthSessionMapper sessionMapper,
            AccountDeletionRequestMapper deletionMapper,
            EmailCodeService codeService,
            SecurityEventService eventService
    ) {
        this.identityMapper = identityMapper;
        this.userMapper = userMapper;
        this.sessionMapper = sessionMapper;
        this.deletionMapper = deletionMapper;
        this.codeService = codeService;
        this.eventService = eventService;
    }

    @Override
    public EmailCodeResponse sendDeleteCode(Long userId, HttpServletRequest req) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != AuthConstants.USER_ACTIVE) {
            throw new BusinessException("当前账号不可注销");
        }

        UserIdentity email = identityMapper.selectByUserIdAndType(userId, AuthConstants.IDENTITY_EMAIL);
        if (email == null || email.getVerified() == null || email.getVerified() != 1) {
            throw new BusinessException("当前账号未绑定邮箱");
        }

        return codeService.sendCode(
                AuthConstants.SCENE_DELETE_ACCOUNT,
                email.getNormalizedValue(),
                req
        );
    }

    @Override
    @Transactional
    public void requestDelete(Long userId, AccountDeleteRequest r, HttpServletRequest req) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != AuthConstants.USER_ACTIVE) {
            throw new BusinessException("当前账号不可注销");
        }

        AccountDeletionRequest exists = deletionMapper.selectPendingByUserId(userId);
        if (exists != null) {
            throw new BusinessException("账号已处于注销冷静期内");
        }

        UserIdentity email = identityMapper.selectByUserIdAndType(userId, AuthConstants.IDENTITY_EMAIL);
        if (email == null || email.getVerified() == null || email.getVerified() != 1) {
            throw new BusinessException("当前账号未绑定邮箱");
        }

        codeService.verifyCode(
                AuthConstants.SCENE_DELETE_ACCOUNT,
                email.getNormalizedValue(),
                r.emailCode()
        );

        AccountDeletionRequest adr = new AccountDeletionRequest();
        adr.setUserId(userId);
        adr.setStatus(AuthConstants.DELETION_PENDING);
        adr.setReason(r.reason());
        adr.setCooldownUntil(LocalDateTime.now().plusDays(deleteCooldownDays));

        deletionMapper.insert(adr);

        userMapper.updateStatusIfCurrent(
                userId,
                AuthConstants.USER_ACTIVE,
                AuthConstants.USER_PENDING_DELETION
        );

        sessionMapper.revokeAllByUserId(userId);

        eventService.log(
                userId,
                AuthConstants.EVENT_DELETE_REQUEST,
                AuthConstants.RESULT_SUCCESS,
                req,
                "pending-delete,cooldownDays=" + deleteCooldownDays
        );
    }

    @Override
    public EmailCodeResponse sendCancelDeleteCode(EmailRequest request, HttpServletRequest servletRequest) {
        String email = NormalizeUtil.normalizeEmail(request.email());

        UserIdentity identity = identityMapper.selectByTypeAndValue(
                AuthConstants.IDENTITY_EMAIL,
                email
        );

        if (identity == null) {
            return new EmailCodeResponse("如果账号处于注销冷静期，我们已发送取消注销验证码", null);
        }

        User user = userMapper.selectById(identity.getUserId());
        if (user == null || user.getStatus() == null || user.getStatus() != AuthConstants.USER_PENDING_DELETION) {
            return new EmailCodeResponse("如果账号处于注销冷静期，我们已发送取消注销验证码", null);
        }

        AccountDeletionRequest pending = deletionMapper.selectPendingByUserId(user.getId());
        if (pending == null) {
            return new EmailCodeResponse("如果账号处于注销冷静期，我们已发送取消注销验证码", null);
        }

        codeService.sendCode(
                AuthConstants.SCENE_CANCEL_DELETE_ACCOUNT,
                email,
                servletRequest
        );

        return new EmailCodeResponse("如果账号处于注销冷静期，我们已发送取消注销验证码", null);
    }

    @Override
    @Transactional
    public void cancelDelete(AccountDeleteCancelRequest request, HttpServletRequest servletRequest) {
        String email = NormalizeUtil.normalizeEmail(request.email());

        codeService.verifyCode(
                AuthConstants.SCENE_CANCEL_DELETE_ACCOUNT,
                email,
                request.emailCode()
        );

        UserIdentity identity = identityMapper.selectByTypeAndValue(
                AuthConstants.IDENTITY_EMAIL,
                email
        );

        if (identity == null) {
            throw new BusinessException("验证码错误或已过期");
        }

        User user = userMapper.selectById(identity.getUserId());
        if (user == null || user.getStatus() == null || user.getStatus() != AuthConstants.USER_PENDING_DELETION) {
            throw new BusinessException("账号不处于注销冷静期");
        }

        AccountDeletionRequest pending = deletionMapper.selectPendingByUserId(user.getId());
        if (pending == null) {
            throw new BusinessException("账号不处于注销冷静期");
        }

        int updatedUser = userMapper.updateStatusIfCurrent(
                user.getId(),
                AuthConstants.USER_PENDING_DELETION,
                AuthConstants.USER_ACTIVE
        );

        if (updatedUser != 1) {
            throw new BusinessException("取消注销失败，请稍后重试");
        }

        deletionMapper.cancelPendingByUserId(user.getId());

        eventService.log(
                user.getId(),
                AuthConstants.EVENT_DELETE_CANCEL,
                AuthConstants.RESULT_SUCCESS,
                servletRequest,
                email
        );
    }

    @Override
    @Transactional
    public int finalizeDueDeletionRequests() {
        List<AccountDeletionRequest> dueList = deletionMapper.selectDuePending(
                LocalDateTime.now(),
                finalizeBatchSize
        );

        int count = 0;

        for (AccountDeletionRequest request : dueList) {
            Long userId = request.getUserId();

            User user = userMapper.selectById(userId);
            if (user == null) {
                deletionMapper.markCompleted(request.getId());
                continue;
            }

            if (user.getStatus() == null || user.getStatus() != AuthConstants.USER_PENDING_DELETION) {
                deletionMapper.cancelPendingByUserId(userId);
                continue;
            }

            sessionMapper.revokeAllByUserId(userId);

            identityMapper.anonymizeByUserId(userId);

            userMapper.updateStatusIfCurrent(
                    userId,
                    AuthConstants.USER_PENDING_DELETION,
                    AuthConstants.USER_DELETED
            );

            deletionMapper.markCompleted(request.getId());

            eventService.log(
                    userId,
                    AuthConstants.EVENT_DELETE_FINALIZE,
                    AuthConstants.RESULT_SUCCESS,
                    null,
                    "finalized"
            );

            count++;
        }

        return count;
    }
}
```

---

## 十、替换 `AccountController.java`

文件路径：

```text
backend/src/main/java/com/smartblog/controller/AccountController.java
```

直接替换为：

```java
package com.smartblog.controller;

import com.smartblog.common.ApiResponse;
import com.smartblog.dto.request.AccountDeleteCancelRequest;
import com.smartblog.dto.request.AccountDeleteRequest;
import com.smartblog.dto.request.EmailRequest;
import com.smartblog.dto.response.EmailCodeResponse;
import com.smartblog.service.AccountService;
import com.smartblog.util.UserContext;
import com.smartblog.vo.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/delete/code/send")
    public ApiResponse<EmailCodeResponse> sendDeleteCode(HttpServletRequest request) {
        CurrentUser cu = UserContext.get();
        return ApiResponse.success(accountService.sendDeleteCode(cu.userId(), request));
    }

    @PostMapping("/delete/request")
    public ApiResponse<Void> requestDelete(
            @Valid @RequestBody AccountDeleteRequest request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser cu = UserContext.get();
        accountService.requestDelete(cu.userId(), request, servletRequest);
        return ApiResponse.success("账号已进入注销冷静期", null);
    }

    @PostMapping("/delete/cancel/code/send")
    public ApiResponse<EmailCodeResponse> sendCancelDeleteCode(
            @Valid @RequestBody EmailRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(accountService.sendCancelDeleteCode(request, servletRequest));
    }

    @PostMapping("/delete/cancel/confirm")
    public ApiResponse<Void> cancelDelete(
            @Valid @RequestBody AccountDeleteCancelRequest request,
            HttpServletRequest servletRequest
    ) {
        accountService.cancelDelete(request, servletRequest);
        return ApiResponse.success("账号注销已取消，请重新登录", null);
    }
}
```

---

## 十一、修改 `WebMvcConfig.java`

文件路径：

```text
backend/src/main/java/com/smartblog/config/WebMvcConfig.java
```

把拦截器排除路径里加上这两个接口：

```java
"/api/account/delete/cancel/code/send",
"/api/account/delete/cancel/confirm"
```

完整替换为：

```java
package com.smartblog.config;

import com.smartblog.interceptor.JwtAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;

    public WebMvcConfig(JwtAuthInterceptor jwtAuthInterceptor) {
        this.jwtAuthInterceptor = jwtAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/auth/login/password",
                        "/api/auth/email-code/send",
                        "/api/auth/login/email-code/send",
                        "/api/auth/login/email-code/verify",
                        "/api/auth/token/refresh",
                        "/api/auth/password/reset/request",
                        "/api/auth/password/reset/confirm",
                        "/api/account/delete/cancel/code/send",
                        "/api/account/delete/cancel/confirm"
                );
    }
}
```

---

## 十二、启用定时任务

文件路径：

```text
backend/src/main/java/com/smartblog/SmartblogBackendApplication.java
```

替换为：

```java
package com.smartblog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SmartblogBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartblogBackendApplication.class, args);
    }
}
```

如果你的启动类名称不是 `SmartblogBackendApplication`，就只需要在你的启动类上加：

```java
@EnableScheduling
```

---

## 十三、新增最终注销定时任务

新增文件：

```text
backend/src/main/java/com/smartblog/task/AccountDeletionFinalizeTask.java
```

内容：

```java
package com.smartblog.task;

import com.smartblog.service.AccountService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AccountDeletionFinalizeTask {

    private final AccountService accountService;

    public AccountDeletionFinalizeTask(AccountService accountService) {
        this.accountService = accountService;
    }

    @Scheduled(fixedDelayString = "${account.delete.finalize-fixed-delay-ms:600000}")
    public void finalizeDueDeletionRequests() {
        accountService.finalizeDueDeletionRequests();
    }
}
```

默认每 10 分钟执行一次最终注销扫描。

---

## 十四、修改 `application.yml`

在 `application.yml` 里增加：

```yaml
account:
  delete:
    cooldown-days: ${ACCOUNT_DELETE_COOLDOWN_DAYS:7}
    finalize-batch-size: ${ACCOUNT_DELETE_FINALIZE_BATCH_SIZE:100}
    finalize-fixed-delay-ms: ${ACCOUNT_DELETE_FINALIZE_FIXED_DELAY_MS:600000}
```

开发测试时可以临时改成：

```yaml
account:
  delete:
    cooldown-days: ${ACCOUNT_DELETE_COOLDOWN_DAYS:0}
    finalize-batch-size: ${ACCOUNT_DELETE_FINALIZE_BATCH_SIZE:100}
    finalize-fixed-delay-ms: ${ACCOUNT_DELETE_FINALIZE_FIXED_DELAY_MS:30000}
```

这样申请注销后，最多 30 秒左右就会最终注销，方便你测试原邮箱和用户名是否释放。

正式环境建议：

```yaml
account:
  delete:
    cooldown-days: 7
    finalize-batch-size: 100
    finalize-fixed-delay-ms: 600000
```

---

## 十五、替换 `PasswordServiceImpl.java`

目标：

待注销账号不允许通过忘记密码恢复。

文件路径：

```text
backend/src/main/java/com/smartblog/service/impl/PasswordServiceImpl.java
```

直接替换为：

```java
package com.smartblog.service.impl;

import com.smartblog.dto.request.PasswordChangeRequest;
import com.smartblog.dto.request.PasswordResetConfirmRequest;
import com.smartblog.dto.request.PasswordResetRequest;
import com.smartblog.dto.response.EmailCodeResponse;
import com.smartblog.entity.User;
import com.smartblog.entity.UserCredential;
import com.smartblog.entity.UserIdentity;
import com.smartblog.exception.BusinessException;
import com.smartblog.mapper.AuthSessionMapper;
import com.smartblog.mapper.UserCredentialMapper;
import com.smartblog.mapper.UserIdentityMapper;
import com.smartblog.mapper.UserMapper;
import com.smartblog.service.EmailCodeService;
import com.smartblog.service.PasswordService;
import com.smartblog.service.SecurityEventService;
import com.smartblog.util.AuthConstants;
import com.smartblog.util.NormalizeUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordServiceImpl implements PasswordService {

    private final UserIdentityMapper identityMapper;
    private final UserMapper userMapper;
    private final UserCredentialMapper credentialMapper;
    private final AuthSessionMapper sessionMapper;
    private final PasswordEncoder encoder;
    private final EmailCodeService codeService;
    private final SecurityEventService eventService;

    @Value("${security.email-code.expire-minutes:10}")
    private Integer expireMinutes;

    public PasswordServiceImpl(
            UserIdentityMapper identityMapper,
            UserMapper userMapper,
            UserCredentialMapper credentialMapper,
            AuthSessionMapper sessionMapper,
            PasswordEncoder encoder,
            EmailCodeService codeService,
            SecurityEventService eventService
    ) {
        this.identityMapper = identityMapper;
        this.userMapper = userMapper;
        this.credentialMapper = credentialMapper;
        this.sessionMapper = sessionMapper;
        this.encoder = encoder;
        this.codeService = codeService;
        this.eventService = eventService;
    }

    @Override
    public EmailCodeResponse requestReset(PasswordResetRequest r, HttpServletRequest req) {
        String email = NormalizeUtil.normalizeEmail(r.email());

        UserIdentity identity = identityMapper.selectByTypeAndValue(
                AuthConstants.IDENTITY_EMAIL,
                email
        );

        if (identity == null) {
            return new EmailCodeResponse("如果账号存在，我们已发送重置方式", expireMinutes);
        }

        User user = userMapper.selectById(identity.getUserId());

        if (user == null || user.getStatus() == null || user.getStatus() != AuthConstants.USER_ACTIVE) {
            return new EmailCodeResponse("如果账号存在，我们已发送重置方式", expireMinutes);
        }

        codeService.sendCode(AuthConstants.SCENE_RESET_PASSWORD, email, req);

        return new EmailCodeResponse("如果账号存在，我们已发送重置方式", expireMinutes);
    }

    @Override
    @Transactional
    public void confirmReset(PasswordResetConfirmRequest r, HttpServletRequest req) {
        String email = NormalizeUtil.normalizeEmail(r.email());

        codeService.verifyCode(AuthConstants.SCENE_RESET_PASSWORD, email, r.code());

        UserIdentity identity = identityMapper.selectByTypeAndValue(
                AuthConstants.IDENTITY_EMAIL,
                email
        );

        if (identity == null) {
            throw new BusinessException("验证码错误或已过期");
        }

        User user = userMapper.selectById(identity.getUserId());
        if (user == null || user.getStatus() == null || user.getStatus() != AuthConstants.USER_ACTIVE) {
            throw new BusinessException("账号不可用，请先取消注销或联系客服");
        }

        credentialMapper.updatePasswordByUserId(
                identity.getUserId(),
                encoder.encode(r.newPassword())
        );

        sessionMapper.revokeAllByUserId(identity.getUserId());

        eventService.log(
                identity.getUserId(),
                AuthConstants.EVENT_PASSWORD_RESET,
                AuthConstants.RESULT_SUCCESS,
                req,
                email
        );
    }

    @Override
    @Transactional
    public void changePassword(Long userId, PasswordChangeRequest r, HttpServletRequest req) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != AuthConstants.USER_ACTIVE) {
            throw new BusinessException("账号不可用");
        }

        UserCredential c = credentialMapper.selectActivePasswordByUserId(userId);

        if (c == null || !encoder.matches(r.oldPassword(), c.getSecretHash())) {
            throw new BusinessException("旧密码错误");
        }

        credentialMapper.updatePasswordByUserId(
                userId,
                encoder.encode(r.newPassword())
        );

        sessionMapper.revokeAllByUserId(userId);

        eventService.log(
                userId,
                AuthConstants.EVENT_PASSWORD_CHANGE,
                AuthConstants.RESULT_SUCCESS,
                req,
                "change-password"
        );
    }
}
```

---

## 十六、修改 `MailServiceImpl.java`

如果你的 `MailServiceImpl` 中有 `sceneName` 方法，增加：

```java
if (AuthConstants.SCENE_CANCEL_DELETE_ACCOUNT.equals(scene)) {
    return "取消注销账号";
}
```

示例：

```java
private String sceneName(String scene) {
    if (AuthConstants.SCENE_REGISTER_EMAIL.equals(scene)) {
        return "注册账号";
    }
    if (AuthConstants.SCENE_LOGIN_EMAIL.equals(scene)) {
        return "邮箱验证码登录";
    }
    if (AuthConstants.SCENE_RESET_PASSWORD.equals(scene)) {
        return "重置密码";
    }
    if (AuthConstants.SCENE_DELETE_ACCOUNT.equals(scene)) {
        return "注销账号";
    }
    if (AuthConstants.SCENE_CANCEL_DELETE_ACCOUNT.equals(scene)) {
        return "取消注销账号";
    }
    return "邮箱验证";
}
```

---

## 十七、前端最小补充

如果你暂时不做页面，也可以用 Apifox/Postman 测试。

如果要先补 API 方法，在：

```text
frontend/src/api/auth.js
```

新增：

```javascript
export function sendCancelDeleteCodeApi(data) {
  return request.post('/api/account/delete/cancel/code/send', data)
}

export function cancelDeleteApi(data) {
  return request.post('/api/account/delete/cancel/confirm', data)
}
```

后续可以单独做一个 `CancelDelete.vue` 页面。

---

## 十八、测试流程

### 1. 测试取消注销

步骤：

```text
1. 注册账号 test@example.com / username1
2. 登录
3. 申请注销
4. 确认无法登录
5. 调用 /api/account/delete/cancel/code/send，传 email
6. 邮箱收到验证码
7. 调用 /api/account/delete/cancel/confirm，传 email + emailCode
8. 再次登录
```

请求示例：

```http
POST /api/account/delete/cancel/code/send
Content-Type: application/json

{
  "email": "test@example.com"
}
```

```http
POST /api/account/delete/cancel/confirm
Content-Type: application/json

{
  "email": "test@example.com",
  "emailCode": "123456"
}
```

预期：

```text
取消注销成功
users.status 从 2 变回 1
account_deletion_requests.status 从 1 变为 2
用户可以重新登录
```

---

### 2. 测试最终注销和标识释放

开发环境建议先把冷静期改成 0：

```yaml
account:
  delete:
    cooldown-days: 0
    finalize-fixed-delay-ms: 30000
```

步骤：

```text
1. 注册账号 username1 / test@example.com
2. 登录
3. 申请注销
4. 等 30 秒左右
5. 查询数据库
6. 用 username1 / test@example.com 再次注册
```

预期：

```text
users.status = 3
account_deletion_requests.status = 3
user_identities.normalized_value 已变成 deleted_xxx
原用户名和邮箱可以重新注册
```

查询 SQL：

```sql
SELECT id, status
FROM users
ORDER BY id DESC
LIMIT 5;

SELECT
  user_id,
  identity_type,
  identity_value,
  normalized_value,
  verified,
  primary_identity
FROM user_identities
ORDER BY id DESC
LIMIT 20;

SELECT
  user_id,
  status,
  cooldown_until,
  finish_time
FROM account_deletion_requests
ORDER BY id DESC
LIMIT 10;
```

---

### 3. 测试忘记密码对待注销账号的处理

步骤：

```text
1. 注册账号
2. 登录
3. 申请注销
4. 进入忘记密码
5. 输入邮箱，请求重置验证码
```

预期：

```text
页面仍然显示统一文案：
如果账号存在，我们已发送重置方式

但实际不会发送重置密码验证码。
```

如果你已经在邮箱里收到重置码，再申请注销，再提交重置确认：

```text
应该失败，提示账号不可用。
```

---

## 十九、常见问题

### 1. 注销后马上重新注册还是提示邮箱已使用

这是正常的。

如果冷静期没有结束，账号只是 `待注销`，不是 `最终注销`，邮箱和用户名仍然应该被占用。

只有最终注销任务执行完成，并且 `user_identities` 匿名化后，原邮箱和用户名才会释放。

---

### 2. 最终注销任务没有执行

检查：

1. 启动类是否加了 `@EnableScheduling`
2. 是否新增了 `AccountDeletionFinalizeTask`
3. `application.yml` 中是否配置了 `account.delete.finalize-fixed-delay-ms`
4. `account_deletion_requests.cooldown_until` 是否已经小于当前时间
5. `account_deletion_requests.status` 是否为 1
6. `users.status` 是否为 2

---

### 3. 取消注销接口返回 401

检查 `WebMvcConfig` 是否放行了：

```java
"/api/account/delete/cancel/code/send",
"/api/account/delete/cancel/confirm"
```

这两个接口必须允许未登录访问。

---

### 4. 忘记密码仍然给待注销账号发邮件

检查 `PasswordServiceImpl.requestReset()` 是否增加了用户状态判断：

```java
if (user == null || user.getStatus() == null || user.getStatus() != AuthConstants.USER_ACTIVE) {
    return new EmailCodeResponse("如果账号存在，我们已发送重置方式", expireMinutes);
}
```

---

## 二十、建议提交信息

```bash
git add backend/src/main/java
git commit -m "feat: complete account deletion lifecycle"

git add backend/src/main/resources/application.yml
git commit -m "chore: add account deletion schedule config"

git add frontend/src/api
git commit -m "feat: add cancel account deletion APIs"
```

---

## 二十一、本轮验收标准

完成后应满足：

```text
1. 注销申请后，账号进入待注销状态。
2. 待注销账号不能登录。
3. 待注销账号不能通过忘记密码恢复。
4. 待注销账号可以通过邮箱验证码取消注销。
5. 取消注销后账号恢复正常，可以重新登录。
6. 冷静期结束后，定时任务会执行最终注销。
7. 最终注销后 user_identities 被匿名化。
8. 最终注销后原用户名和邮箱可以重新注册。
```
