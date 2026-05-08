# SmartBlog Auth

SmartBlog Auth 是一个基于 **Spring Boot 3.3 + MyBatis + MySQL + Vue 3 (Vite)** 构建的完整账号认证系统，提供从注册登录到账号注销的全生命周期管理能力。

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.3.5 |
| ORM | MyBatis (Spring Boot Starter) | 3.0.4 |
| 数据库 | MySQL | 8.x (utf8mb4) |
| 密码加密 | BCrypt (Spring Security Crypto) | - |
| JWT | jjwt (io.jsonwebtoken) | 0.11.5 |
| 前端框架 | Vue 3 (Composition API) | ^3.5.32 |
| 构建工具 | Vite | ^8.0.4 |
| 路由 | Vue Router | ^4.5.1 |
| HTTP 客户端 | Axios | ^1.15.0 |
| JDK | Java | 17 |

## 功能清单

### 账号注册
- 用户名 + 邮箱注册
- 邮箱验证码发送（带频率限制）
- 用户名/邮箱规范化（统一小写去空格）
- 唯一性校验

### 登录方式
- **账号密码登录**：支持用户名或邮箱 + 密码
- **邮箱验证码登录**：输入邮箱验证码即可登录
- 登录限流保护（5 次失败后 15 分钟内限制）

### Token 与会话管理
- **双 Token 机制**：Access Token（短效，默认 30 分钟）+ Refresh Token（长效，默认 7 天）
- **自动续期**：前端 Axios 拦截器自动检测 401 并静默刷新 Token
- **服务端退出**：单设备退出 / 退出全部设备
- **设备管理**：查看所有活跃会话，支持踢下线指定设备

### 密码管理
- **修改密码**：登录后修改密码，自动退出全部设备
- **忘记密码**：邮箱验证码验证后重置密码

### 账号注销（完整流程）
- **注销申请**：发送邮箱验证码验证身份后提交注销申请
- **冷静期机制**：默认 7 天冷静期（可配置），期间账号置为"待注销"状态，无法登录
- **取消注销**：冷静期内可通过邮箱验证码验证取消注销
- **自动执行**：定时任务扫描到期申请，自动完成最终注销（删除用户数据）
- **安全日志**：记录注销申请、取消、完成等全流程事件

### 安全特性
- 密码 BCrypt 加密存储
- JWT 签名验证
- 邮箱验证码哈希存储
- 验证码发送频率限制（每小时每目标 5 次、每 IP 20 次）
- 验证码错误尝试次数限制（5 次）
- 登录失败限流
- 全局 CORS 配置
- 参数校验（Jakarta Validation）
- 全局异常处理
- 安全事件日志

## 快速开始

### 前置要求
- JDK 17+
- Maven 3.6+
- MySQL 8.x
- Node.js 18+
- 邮箱（用于发送验证码，推荐 163/QQ 邮箱）

### 1. 创建数据库

```sql
CREATE DATABASE IF NOT EXISTS smartblog_auth
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;
```

### 2. 初始化表结构

```bash
# 执行 backend/src/main/resources/sql/schema.sql 中的建表语句
mysql -u root -p smartblog_auth < backend/src/main/resources/sql/schema.sql
```

### 3. 配置后端

复制环境变量模板并编辑：

```bash
cp backend/.env.example backend/.env
```

主要配置项（也可通过系统环境变量设置）：

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `SERVER_PORT` | 服务端口 | 7070 |
| `DB_URL` | 数据库连接地址 | jdbc:mysql://localhost:3306/smartblog_auth?... |
| `DB_USERNAME` | 数据库用户名 | root |
| `DB_PASSWORD` | 数据库密码 | **必填** |
| `JWT_SECRET` | JWT 签名密钥（至少 256 位） | **必填** |
| `MAIL_HOST` | SMTP 服务器 | smtp.163.com |
| `MAIL_PORT` | SMTP 端口 | 465 |
| `MAIL_USERNAME` | 邮箱账号 | **必填** |
| `MAIL_AUTH_CODE` | 邮箱授权码 | **必填** |
| `ACCOUNT_DELETE_COOLDOWN_DAYS` | 注销冷静期天数 | 7 |

### 4. 启动后端

```bash
cd backend
mvn spring-boot:run
```

### 5. 配置并启动前端

```bash
cd frontend

# 复制环境变量模板
cp .env.example .env

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端默认运行在 `http://localhost:5173`，API 代理到 `http://localhost:7070`。

## 项目结构

```
smartblog-auth-local-fixed/
├── backend/                          # 后端 (Spring Boot)
│   ├── src/main/java/com/smartblog/
│   │   ├── common/                   # 通用响应封装
│   │   │   └── ApiResponse.java
│   │   ├── config/                   # 配置类
│   │   │   ├── CorsConfig.java       # 跨域配置
│   │   │   ├── PasswordConfig.java   # BCrypt 密码编码器
│   │   │   └── WebMvcConfig.java     # 拦截器注册
│   │   ├── controller/               # 控制器
│   │   │   ├── AuthController.java   # 认证相关接口
│   │   │   ├── AccountController.java # 账号注销相关接口
│   │   │   └── UserController.java   # 用户信息接口
│   │   ├── dto/                      # 数据传输对象
│   │   │   ├── request/              # 请求 DTO
│   │   │   └── response/             # 响应 DTO
│   │   ├── entity/                   # 数据实体
│   │   │   ├── User.java
│   │   │   ├── UserIdentity.java
│   │   │   ├── UserCredential.java
│   │   │   ├── AuthSession.java
│   │   │   ├── VerificationChallenge.java
│   │   │   ├── SecurityEvent.java
│   │   │   └── AccountDeletionRequest.java
│   │   ├── exception/                # 异常处理
│   │   │   ├── BusinessException.java
│   │   │   └── GlobalExceptionHandler.java
│   │   ├── interceptor/              # 拦截器
│   │   │   └── JwtAuthInterceptor.java
│   │   ├── mapper/                   # MyBatis Mapper
│   │   ├── service/                  # 服务层接口与实现
│   │   │   ├── impl/                 # 服务实现
│   │   │   └── ...
│   │   ├── task/                     # 定时任务
│   │   │   └── AccountDeletionFinalizeTask.java
│   │   ├── util/                     # 工具类
│   │   │   ├── AuthConstants.java    # 常量定义
│   │   │   ├── JwtUtil.java          # JWT 工具
│   │   │   ├── NormalizeUtil.java    # 规范化工具
│   │   │   ├── RequestUtil.java      # 请求工具
│   │   │   └── UserContext.java      # 用户上下文
│   │   └── vo/                       # 值对象
│   │       └── CurrentUser.java
│   └── src/main/resources/
│       ├── mapper/                   # XML Mapper
│       ├── sql/schema.sql            # 数据库建表脚本
│       └── application.yml           # 应用配置
├── frontend/                         # 前端 (Vue 3)
│   └── src/
│       ├── api/auth.js               # API 接口封装
│       ├── assets/styles/            # 全局样式
│       ├── router/index.js           # 路由配置
│       ├── utils/
│       │   ├── auth.js               # Token 管理
│       │   └── request.js            # Axios 封装（自动刷新 Token）
│       └── views/                    # 页面组件
│           ├── Home.vue              # 首页
│           ├── Login.vue             # 登录（密码/验证码双模式）
│           ├── Register.vue          # 注册
│           ├── Profile.vue           # 个人中心（修改密码）
│           ├── ForgotPassword.vue    # 忘记密码
│           ├── DeviceManage.vue      # 设备管理
│           ├── DeleteAccount.vue     # 注销账号
│           └── CancelDelete.vue      # 取消注销
└── docs/                             # 文档
    ├── SmartBlog账号认证系统从零搭建与功能测试流程.md
    ├── SmartBlog账号注销流程完善实施指南.md
    └── SmartBlog账号注销流程前端补充实施指南.md
```

## API 接口一览

### 认证接口 (`/api/auth`)

| 方法 | 路径 | 说明 | 需登录 |
|------|------|------|--------|
| POST | `/email-code/send` | 发送注册邮箱验证码 | 否 |
| POST | `/register` | 注册账号 | 否 |
| POST | `/login/password` | 账号密码登录 | 否 |
| POST | `/login/email-code/send` | 发送登录邮箱验证码 | 否 |
| POST | `/login/email-code/verify` | 邮箱验证码登录 | 否 |
| POST | `/token/refresh` | 刷新 Token | 否 |
| POST | `/logout` | 退出当前设备 | 是 |
| POST | `/logout-all` | 退出全部设备 | 是 |
| POST | `/password/reset/request` | 忘记密码-发送验证码 | 否 |
| POST | `/password/reset/confirm` | 忘记密码-确认重置 | 否 |
| POST | `/password/change` | 修改密码 | 是 |
| GET  | `/sessions` | 获取会话列表 | 是 |
| DELETE | `/sessions/{sessionId}` | 踢下线指定设备 | 是 |

### 账号注销接口 (`/api/account`)

| 方法 | 路径 | 说明 | 需登录 |
|------|------|------|--------|
| POST | `/delete/code/send` | 发送注销验证码 | 是 |
| POST | `/delete/request` | 提交注销申请 | 是 |
| POST | `/delete/cancel/code/send` | 发送取消注销验证码 | 否 |
| POST | `/delete/cancel/confirm` | 确认取消注销 | 否 |

### 用户接口 (`/api/user`)

| 方法 | 路径 | 说明 | 需登录 |
|------|------|------|--------|
| GET  | `/me` | 获取当前用户信息 | 是 |

## 数据库设计

共 7 张表：

| 表名 | 说明 |
|------|------|
| `users` | 用户主体表（状态：正常/禁用/待注销/已注销） |
| `user_identities` | 用户登录标识表（用户名/邮箱，支持多标识） |
| `user_credentials` | 用户认证凭据表（密码哈希） |
| `auth_sessions` | 登录会话表（设备信息、Token 管理） |
| `verification_challenges` | 验证码挑战表（邮箱验证码） |
| `security_events` | 安全事件日志表 |
| `account_deletion_requests` | 账号注销申请表（冷静期管理） |

## 环境变量参考

参见各模块下的 `.env.example` 文件：

- [backend/.env.example](backend/.env.example)
- [frontend/.env.example](frontend/.env.example)

## 详细文档

- [SmartBlog 账号认证系统从零搭建与功能测试流程](docs/SmartBlog账号认证系统从零搭建与功能测试流程.md)
- [SmartBlog 账号注销流程完善实施指南](docs/SmartBlog账号注销流程完善实施指南.md)
- [SmartBlog 账号注销流程前端补充实施指南](docs/SmartBlog账号注销流程前端补充实施指南.md)
