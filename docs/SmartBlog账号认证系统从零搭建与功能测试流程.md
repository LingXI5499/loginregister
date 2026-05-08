# SmartBlog 账号认证系统从零搭建与功能测试流程

本文档说明如何从空目录开始，在 IntelliJ IDEA 与 VS Code 中搭建 SmartBlog 账号认证系统，并完成核心功能测试。它不是 README 摘要，而是面向开发与验收的完整流程文档。

## 1. 项目目标

本项目实现一个基础版账号认证系统，覆盖以下能力：

1. 用户名/邮箱注册
2. 账号密码登录，账号字段支持用户名或邮箱
3. 邮箱验证码登录
4. 忘记密码与重置密码
5. access token + refresh token 会话管理
6. 服务端 logout 与 logout-all
7. 登录失败限流
8. 安全事件追踪
9. 设备管理与指定设备下线
10. 修改密码并撤销旧会话
11. 注销流程基础版

## 2. 技术栈

后端：

- Java 17
- Spring Boot 3.3.5
- Spring MVC
- Bean Validation
- MyBatis
- MySQL 8.x
- BCrypt
- JJWT

前端：

- Vue 3
- Vue Router
- Axios
- Vite

数据库：

- MySQL 数据库名：`smartblog_auth`

## 3. 从零创建后端工程

### 3.1 在 IDEA 创建 Spring Boot 工程

1. 打开 IntelliJ IDEA。
2. 选择 `New Project`。
3. 选择 `Spring Initializr`。
4. JDK 选择 Java 17。
5. 项目信息建议填写：
   - Group：`com.smartblog`
   - Artifact：`smartblog-backend`
   - Name：`smartblog-backend`
   - Package name：`com.smartblog`
6. 选择依赖：
   - Spring Web
   - Validation
   - MySQL Driver
   - MyBatis Framework
7. 创建完成后，在 `pom.xml` 中补充：
   - `spring-security-crypto`：用于 BCrypt
   - `jjwt-api`、`jjwt-impl`、`jjwt-jackson`：用于 JWT

### 3.2 后端目录组织

建议按以下结构组织：

```text
backend/src/main/java/com/smartblog
├── common
├── config
├── controller
├── dto
│   ├── request
│   └── response
├── entity
├── exception
├── interceptor
├── mapper
├── service
│   └── impl
├── util
└── vo
```

### 3.3 创建数据库

进入 MySQL 后执行：

```sql
SOURCE backend/src/main/resources/sql/schema.sql;
```

该脚本会创建并重建以下表：

| 表名 | 用途 |
|---|---|
| `users` | 用户主体表 |
| `user_identities` | 用户名、邮箱等登录标识 |
| `user_credentials` | 密码哈希等认证凭据 |
| `auth_sessions` | access/refresh 会话与设备信息 |
| `verification_challenges` | 邮箱验证码挑战 |
| `security_events` | 登录、退出、改密、注销等安全事件 |
| `account_deletion_requests` | 注销申请记录 |

### 3.4 配置后端连接

编辑：

```text
backend/src/main/resources/application.yml
```

重点确认：

```yaml
server:
  port: 7070

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/smartblog_auth?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8&allowPublicKeyRetrieval=true
    username: root
    password: 1234
```

如你的 MySQL 密码不同，只修改 `password` 即可。

### 3.5 启动后端

在 IDEA 中打开：

```text
SmartblogBackendApplication.java
```

点击运行。看到 Spring Boot 启动并监听 `7070` 端口后，后端启动完成。

## 4. 从零创建前端工程

### 4.1 在 VS Code 创建 Vue 工程

可以用以下方式创建：

```bash
npm create vite@latest frontend -- --template vue
cd frontend
npm install
npm install axios vue-router
```

本项目已经提供完整前端代码，打开 `frontend` 目录即可继续开发。

### 4.2 前端目录组织

```text
frontend/src
├── api
├── assets
│   └── styles
├── router
├── utils
└── views
```

### 4.3 前端接口代理

`vite.config.js` 中配置了 `/api` 代理到后端 `http://localhost:7070`。前端请求统一写成：

```text
/api/auth/...
/api/user/...
/api/account/...
```

### 4.4 启动前端

在 VS Code 终端执行：

```bash
cd frontend
npm install
npm run dev
```

默认访问：

```text
http://localhost:5173
```

## 5. 核心接口清单

### 5.1 邮箱验证码

| 方法 | 地址 | 说明 |
|---|---|---|
| POST | `/api/auth/email-code/send` | 注册邮箱验证码 |
| POST | `/api/auth/login/email-code/send` | 邮箱验证码登录发送验证码 |
| POST | `/api/auth/password/reset/request` | 忘记密码发送验证码 |
| POST | `/api/account/delete/code/send` | 注销账号发送验证码 |

当前工程未接真实 SMTP 网关，接口会返回 `debugCode`，方便完整验收验证码流程。接入真实邮件服务时，应移除响应中的 `debugCode`，并在 `EmailCodeServiceImpl` 中替换邮件发送实现。

### 5.2 认证接口

| 方法 | 地址 | 说明 |
|---|---|---|
| POST | `/api/auth/register` | 用户名/邮箱注册 |
| POST | `/api/auth/login/password` | 用户名或邮箱 + 密码登录 |
| POST | `/api/auth/login/email-code/verify` | 邮箱验证码登录 |
| POST | `/api/auth/token/refresh` | 刷新 access token |
| POST | `/api/auth/logout` | 退出当前设备 |
| POST | `/api/auth/logout-all` | 退出全部设备 |

### 5.3 密码接口

| 方法 | 地址 | 说明 |
|---|---|---|
| POST | `/api/auth/password/reset/request` | 发起密码重置 |
| POST | `/api/auth/password/reset/confirm` | 确认重置密码 |
| POST | `/api/auth/password/change` | 登录后修改密码 |

### 5.4 设备与注销接口

| 方法 | 地址 | 说明 |
|---|---|---|
| GET | `/api/auth/sessions` | 查询有效设备列表 |
| DELETE | `/api/auth/sessions/{sessionId}` | 踢下线指定设备 |
| POST | `/api/account/delete/request` | 发起注销基础流程 |

## 6. 功能测试流程

以下测试可以用前端页面，也可以用 Postman。

### 6.1 测试注册

第一步，发送注册验证码：

```http
POST http://localhost:7070/api/auth/email-code/send
Content-Type: application/json

{
  "email": "test@example.com"
}
```

记录响应里的：

```json
"debugCode": "123456"
```

第二步，注册账号：

```http
POST http://localhost:7070/api/auth/register
Content-Type: application/json

{
  "username": "test001",
  "email": "test@example.com",
  "emailCode": "上一步返回的验证码",
  "password": "123456",
  "nickname": "测试用户"
}
```

验收点：

- `users` 生成用户主体
- `user_identities` 生成 USERNAME 和 EMAIL 两条标识
- EMAIL 的 `verified = 1`
- `user_credentials` 生成 BCrypt 密码哈希
- `security_events` 生成 REGISTER 记录

### 6.2 测试账号密码登录

```http
POST http://localhost:7070/api/auth/login/password
Content-Type: application/json

{
  "account": "test001",
  "password": "123456",
  "deviceName": "Postman"
}
```

也可以用邮箱登录：

```json
{
  "account": "test@example.com",
  "password": "123456",
  "deviceName": "Postman"
}
```

验收点：

- 响应返回 `accessToken`
- 响应返回 `refreshToken`
- `auth_sessions` 生成有效会话
- `security_events` 生成 LOGIN_SUCCESS

### 6.3 测试登录失败限流

连续多次用错误密码调用登录接口。

验收点：

- 登录失败统一返回“账号或密码错误”
- 达到阈值后返回“登录失败次数过多，请稍后再试”
- `security_events` 生成 LOGIN_FAIL 记录

### 6.4 测试邮箱验证码登录

第一步：

```http
POST http://localhost:7070/api/auth/login/email-code/send
Content-Type: application/json

{
  "email": "test@example.com"
}
```

第二步：

```http
POST http://localhost:7070/api/auth/login/email-code/verify
Content-Type: application/json

{
  "email": "test@example.com",
  "code": "上一步返回的验证码",
  "deviceName": "Postman Email Login"
}
```

验收点：

- 验证码只能使用一次
- 错误验证码会增加错误次数
- 登录成功会创建新会话

### 6.5 测试 refresh token

```http
POST http://localhost:7070/api/auth/token/refresh
Content-Type: application/json

{
  "refreshToken": "登录返回的 refreshToken"
}
```

验收点：

- 返回新的 `accessToken`
- `auth_sessions.access_token_jti` 被更新
- 旧 access token 访问受保护接口会被拒绝

### 6.6 测试当前用户信息

```http
GET http://localhost:7070/api/user/me
Authorization: Bearer accessToken
```

验收点：

- 返回用户 ID、用户名、邮箱、邮箱验证状态、昵称、账号状态

### 6.7 测试设备管理

```http
GET http://localhost:7070/api/auth/sessions
Authorization: Bearer accessToken
```

验收点：

- 返回当前用户的有效设备列表
- 当前设备标记为 `current = true`

踢下线设备：

```http
DELETE http://localhost:7070/api/auth/sessions/{sessionId}
Authorization: Bearer accessToken
```

### 6.8 测试服务端退出

退出当前设备：

```http
POST http://localhost:7070/api/auth/logout
Authorization: Bearer accessToken
```

退出全部设备：

```http
POST http://localhost:7070/api/auth/logout-all
Authorization: Bearer accessToken
```

验收点：

- `auth_sessions.status` 变为 0
- 旧 access token 再访问 `/api/user/me` 返回 401

### 6.9 测试忘记密码

第一步：

```http
POST http://localhost:7070/api/auth/password/reset/request
Content-Type: application/json

{
  "email": "test@example.com"
}
```

第二步：

```http
POST http://localhost:7070/api/auth/password/reset/confirm
Content-Type: application/json

{
  "email": "test@example.com",
  "code": "上一步验证码",
  "newPassword": "new123456"
}
```

验收点：

- 密码哈希更新
- 该用户全部旧 session 被撤销
- 旧 access token 无法继续使用

### 6.10 测试修改密码

```http
POST http://localhost:7070/api/auth/password/change
Authorization: Bearer accessToken
Content-Type: application/json

{
  "oldPassword": "new123456",
  "newPassword": "new654321"
}
```

验收点：

- 必须校验旧密码
- 修改成功后撤销全部旧会话
- 前端会清空 token 并跳转登录页

### 6.11 测试注销流程基础版

第一步：

```http
POST http://localhost:7070/api/account/delete/code/send
Authorization: Bearer accessToken
```

第二步：

```http
POST http://localhost:7070/api/account/delete/request
Authorization: Bearer accessToken
Content-Type: application/json

{
  "emailCode": "上一步验证码",
  "reason": "不再使用"
}
```

验收点：

- `account_deletion_requests` 新增记录
- `users.status` 变为 2
- 该用户所有 session 被撤销
- 该账号不能继续登录

## 7. 前端页面验收

| 页面 | 路由 | 验收点 |
|---|---|---|
| 首页 | `/` | 可以跳转登录、注册、个人中心、设备管理 |
| 注册 | `/register` | 可发送邮箱验证码并注册 |
| 登录 | `/login` | 支持账号密码登录和邮箱验证码登录 |
| 忘记密码 | `/forgot-password` | 可发送验证码并重置密码 |
| 个人中心 | `/profile` | 展示当前用户、修改密码、退出登录、退出全部设备 |
| 设备管理 | `/devices` | 展示会话列表并踢下线其他设备 |
| 注销账号 | `/delete-account` | 发送注销验证码并发起注销 |

## 8. 下一步扩展建议

当前版本已经具备基础认证系统能力。后续可以继续扩展：

1. 接入真实 SMTP 邮件发送服务
2. 增加手机号验证码注册/登录
3. 增加邮箱绑定/换绑流程
4. 增加第三方登录表 `oauth_accounts`
5. 增加 TOTP MFA
6. 增加 Passkey/WebAuthn
7. 增加后台账号冻结/解冻管理
8. 增加注销冷静期与数据匿名化任务
