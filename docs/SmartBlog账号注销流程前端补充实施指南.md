# SmartBlog 账号注销流程前端补充实施指南

本文档补充上一版《SmartBlog账号注销流程完善实施指南》中缺失的前端部分，目标是让前端支持：

1. 注销后提示“账号进入注销冷静期”
2. 提供“取消注销”页面
3. 用户可以输入邮箱，发送取消注销验证码
4. 用户可以输入邮箱验证码，取消注销
5. 取消注销成功后跳转登录页
6. 忘记密码页面对“待注销账号”保持统一提示，不额外暴露账号状态

本文档默认你的前端是 Vue 3 + Vue Router + Axios，目录大致如下：

```text
frontend/src/api/auth.js
frontend/src/router/index.js
frontend/src/views/DeleteAccount.vue
frontend/src/views/ForgotPassword.vue
frontend/src/views/Login.vue
frontend/src/views/CancelDelete.vue
```

如果你的文件名略有不同，按实际项目路径调整即可。

---

## 一、后端接口回顾

后端新增两个免登录接口：

```http
POST /api/account/delete/cancel/code/send
POST /api/account/delete/cancel/confirm
```

### 1. 发送取消注销验证码

请求：

```json
{
  "email": "test@example.com"
}
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "message": "如果账号处于注销冷静期，我们已发送取消注销验证码",
    "expiresInMinutes": null
  }
}
```

说明：

这里使用统一文案，避免暴露该邮箱是否真的处于注销冷静期。

---

### 2. 确认取消注销

请求：

```json
{
  "email": "test@example.com",
  "emailCode": "123456"
}
```

成功响应：

```json
{
  "code": 200,
  "message": "账号注销已取消，请重新登录",
  "data": null
}
```

---

## 二、修改前端 API 文件

文件路径：

```text
frontend/src/api/auth.js
```

在文件末尾增加：

```javascript
export function sendCancelDeleteCodeApi(data) {
  return request.post('/api/account/delete/cancel/code/send', data)
}

export function cancelDeleteApi(data) {
  return request.post('/api/account/delete/cancel/confirm', data)
}
```

如果你的项目把账号相关接口放在：

```text
frontend/src/api/account.js
```

那就加到 `account.js` 里，引用时对应改路径即可。

---

## 三、新增取消注销页面 `CancelDelete.vue`

新增文件：

```text
frontend/src/views/CancelDelete.vue
```

内容：

```vue
<template>
  <div class="page">
    <div class="card">
      <h1>取消账号注销</h1>

      <p class="desc">
        如果你的账号仍处于注销冷静期，可以通过邮箱验证码取消注销。
        取消成功后，请重新登录。
      </p>

      <div class="form">
        <label>邮箱</label>
        <input
          v-model.trim="form.email"
          type="email"
          placeholder="请输入注册邮箱"
          autocomplete="email"
        />

        <label>邮箱验证码</label>
        <div class="code-row">
          <input
            v-model.trim="form.emailCode"
            type="text"
            maxlength="6"
            placeholder="请输入6位验证码"
            autocomplete="one-time-code"
          />
          <button
            type="button"
            :disabled="codeLoading || countdown > 0"
            @click="sendCode"
          >
            {{ codeButtonText }}
          </button>
        </div>

        <button
          class="primary"
          type="button"
          :disabled="loading"
          @click="submit"
        >
          {{ loading ? '处理中...' : '取消注销' }}
        </button>
      </div>

      <div class="links">
        <RouterLink to="/login">返回登录</RouterLink>
        <RouterLink to="/forgot-password">忘记密码</RouterLink>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { sendCancelDeleteCodeApi, cancelDeleteApi } from '@/api/auth'

const router = useRouter()

const loading = ref(false)
const codeLoading = ref(false)
const countdown = ref(0)
let timer = null

const form = reactive({
  email: '',
  emailCode: ''
})

const codeButtonText = computed(() => {
  if (codeLoading.value) return '发送中...'
  if (countdown.value > 0) return `${countdown.value}s`
  return '发送验证码'
})

function validateEmail(email) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
}

function startCountdown(seconds = 60) {
  countdown.value = seconds
  clearInterval(timer)
  timer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      clearInterval(timer)
      timer = null
    }
  }, 1000)
}

async function sendCode() {
  if (!form.email) {
    alert('请先输入邮箱')
    return
  }

  if (!validateEmail(form.email)) {
    alert('邮箱格式不正确')
    return
  }

  codeLoading.value = true

  try {
    const res = await sendCancelDeleteCodeApi({
      email: form.email
    })

    alert(res.data?.message || res.message || '如果账号处于注销冷静期，我们已发送取消注销验证码')
    startCountdown(60)
  } finally {
    codeLoading.value = false
  }
}

async function submit() {
  if (!form.email) {
    alert('请输入邮箱')
    return
  }

  if (!validateEmail(form.email)) {
    alert('邮箱格式不正确')
    return
  }

  if (!/^\d{6}$/.test(form.emailCode)) {
    alert('请输入6位邮箱验证码')
    return
  }

  loading.value = true

  try {
    const res = await cancelDeleteApi({
      email: form.email,
      emailCode: form.emailCode
    })

    alert(res.message || '账号注销已取消，请重新登录')
    router.push('/login')
  } finally {
    loading.value = false
  }
}

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
  background: #f5f7fb;
}

.card {
  width: 100%;
  max-width: 420px;
  padding: 28px;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
}

h1 {
  margin: 0 0 12px;
  font-size: 24px;
  color: #111827;
}

.desc {
  margin: 0 0 24px;
  line-height: 1.7;
  color: #6b7280;
  font-size: 14px;
}

.form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

label {
  font-size: 14px;
  color: #374151;
}

input {
  height: 42px;
  padding: 0 12px;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  outline: none;
  font-size: 14px;
}

input:focus {
  border-color: #2563eb;
}

.code-row {
  display: flex;
  gap: 10px;
}

.code-row input {
  flex: 1;
}

button {
  height: 42px;
  padding: 0 14px;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  background: #e5e7eb;
  color: #111827;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

.primary {
  margin-top: 8px;
  width: 100%;
  background: #2563eb;
  color: white;
}

.links {
  display: flex;
  justify-content: space-between;
  margin-top: 18px;
  font-size: 14px;
}

.links a {
  color: #2563eb;
  text-decoration: none;
}
</style>
```

---

## 四、修改路由配置

文件路径：

```text
frontend/src/router/index.js
```

在 routes 中新增：

```javascript
{
  path: '/cancel-delete',
  name: 'CancelDelete',
  component: () => import('@/views/CancelDelete.vue'),
  meta: {
    public: true
  }
}
```

示例：

```javascript
import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: {
      public: true
    }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: {
      public: true
    }
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('@/views/ForgotPassword.vue'),
    meta: {
      public: true
    }
  },
  {
    path: '/cancel-delete',
    name: 'CancelDelete',
    component: () => import('@/views/CancelDelete.vue'),
    meta: {
      public: true
    }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
```

如果你的项目已经有路由守卫，请确认 `/cancel-delete` 是免登录页面。

---

## 五、如果你的路由守卫用白名单

如果 `router/index.js` 里有类似：

```javascript
const whiteList = ['/login', '/register', '/forgot-password']
```

请改成：

```javascript
const whiteList = [
  '/login',
  '/register',
  '/forgot-password',
  '/cancel-delete'
]
```

否则取消注销页面会被跳转到登录页。

---

## 六、修改登录页，增加“取消注销”入口

文件路径：

```text
frontend/src/views/Login.vue
```

在登录页底部链接区加入：

```vue
<RouterLink to="/cancel-delete">取消注销</RouterLink>
```

例如原来是：

```vue
<div class="links">
  <RouterLink to="/register">注册账号</RouterLink>
  <RouterLink to="/forgot-password">忘记密码</RouterLink>
</div>
```

改成：

```vue
<div class="links">
  <RouterLink to="/register">注册账号</RouterLink>
  <RouterLink to="/forgot-password">忘记密码</RouterLink>
  <RouterLink to="/cancel-delete">取消注销</RouterLink>
</div>
```

如果样式太挤，可以改成纵向：

```css
.links {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 18px;
}
```

或者保持横向换行：

```css
.links {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 18px;
}
```

---

## 七、修改注销页 `DeleteAccount.vue`

目标：

1. 注销成功后，不要直接让用户以为已经永久删除。
2. 明确提示“账号已进入注销冷静期”。
3. 给用户提供“取消注销”入口。

文件路径：

```text
frontend/src/views/DeleteAccount.vue
```

找到提交注销成功后的逻辑，类似：

```javascript
alert('账号已注销')
router.push('/login')
```

改成：

```javascript
alert('账号已进入注销冷静期。在冷静期结束前，你可以通过邮箱验证码取消注销。')
router.push('/cancel-delete')
```

如果你想让用户回登录页，也可以：

```javascript
alert('账号已进入注销冷静期。在冷静期结束前，你可以在登录页点击“取消注销”恢复账号。')
router.push('/login')
```

建议用第一种，直接跳到取消注销页面。

---

## 八、修改忘记密码页 `ForgotPassword.vue`

后端已经做了统一文案处理：

```text
如果账号不存在，返回统一文案。
如果账号处于待注销状态，也返回统一文案，但不发邮件。
```

前端不需要判断账号状态，只需要保持统一提示即可。

建议在页面说明里加一句：

```text
如果账号处于注销冷静期，请先取消注销后再重置密码。
```

例如在模板中加入：

```vue
<p class="tip">
  如果账号处于注销冷静期，请先
  <RouterLink to="/cancel-delete">取消注销</RouterLink>
  后再重置密码。
</p>
```

如果你不想暴露太多，也可以写得更温和：

```vue
<p class="tip">
  如果无法收到重置邮件，请确认账号状态，或尝试取消注销。
</p>
```

样式：

```css
.tip {
  margin-top: 12px;
  color: #6b7280;
  font-size: 14px;
  line-height: 1.6;
}

.tip a {
  color: #2563eb;
  text-decoration: none;
}
```

---

## 九、修改请求拦截器，避免取消注销接口被强行带旧 token

文件路径可能是：

```text
frontend/src/utils/request.js
```

或者：

```text
frontend/src/api/request.js
```

如果你的请求拦截器当前是：

```javascript
request.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})
```

一般可以不改，因为取消注销接口后端已经放行。

但如果你注销后没有清理本地 token，可能会出现旧 token 影响体验。

建议在注销成功后清理本地 token：

```javascript
localStorage.removeItem('accessToken')
localStorage.removeItem('refreshToken')
```

如果你的 token key 是别的名字，按实际项目调整。

在 `DeleteAccount.vue` 的注销成功逻辑里加：

```javascript
localStorage.removeItem('accessToken')
localStorage.removeItem('refreshToken')
alert('账号已进入注销冷静期。在冷静期结束前，你可以通过邮箱验证码取消注销。')
router.push('/cancel-delete')
```

---

## 十、如果项目有 Pinia / Vuex 用户状态

如果你的项目使用 Pinia，例如：

```text
frontend/src/stores/user.js
```

注销成功后需要清理用户状态。

示例：

```javascript
const userStore = useUserStore()
userStore.clearUser()
```

然后再跳转：

```javascript
router.push('/cancel-delete')
```

如果你当前项目没有 Pinia/Vuex，可以忽略。

---

## 十一、建议新增页面入口

除了登录页，也可以在以下位置增加入口：

### 1. 忘记密码页

```vue
<RouterLink to="/cancel-delete">账号处于注销冷静期？取消注销</RouterLink>
```

### 2. 注销成功后跳转页面

直接跳：

```javascript
router.push('/cancel-delete')
```

### 3. 登录失败提示旁边

如果登录接口返回“账号不可用”，可以提示：

```text
账号可能处于注销冷静期，如需恢复请点击取消注销。
```

但不建议根据具体错误自动判断太多，避免暴露账号状态。

---

## 十二、手动测试流程

### 1. 取消注销流程

步骤：

```text
1. 注册账号
2. 登录账号
3. 进入注销页面
4. 发送注销验证码
5. 提交注销申请
6. 页面跳转到 /cancel-delete
7. 输入邮箱
8. 发送取消注销验证码
9. 输入验证码
10. 点击取消注销
11. 跳转登录页
12. 使用原账号密码登录
```

预期：

```text
账号恢复正常。
users.status 从 2 变为 1。
account_deletion_requests.status 从 1 变为 2。
```

---

### 2. 最终注销后重新注册流程

开发环境建议后端配置：

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
4. 不取消注销
5. 等待 30 秒以上
6. 使用 username1 / test@example.com 再次注册
```

预期：

```text
重新注册成功。
原用户的 user_identities 已被匿名化。
```

---

### 3. 忘记密码对待注销账号

步骤：

```text
1. 注册账号
2. 申请注销
3. 打开忘记密码页面
4. 输入同一个邮箱
5. 点击发送重置验证码
```

预期：

```text
前端显示统一文案。
邮箱不会收到重置密码验证码。
页面提示可以尝试取消注销。
```

---

## 十三、常见问题

### 1. 访问 `/cancel-delete` 被重定向到登录页

检查路由白名单是否加入：

```javascript
'/cancel-delete'
```

如果使用 `meta.public`，检查路由守卫是否正确读取。

---

### 2. 取消注销接口返回 401

检查后端 `WebMvcConfig` 是否放行：

```java
"/api/account/delete/cancel/code/send",
"/api/account/delete/cancel/confirm"
```

---

### 3. 取消注销验证码收不到

检查后端 `MailServiceImpl.sceneName()` 是否加入：

```java
SCENE_CANCEL_DELETE_ACCOUNT
```

检查接口请求体是否是：

```json
{
  "email": "test@example.com"
}
```

---

### 4. 注销后页面仍显示已登录用户

注销成功后需要清理本地 token：

```javascript
localStorage.removeItem('accessToken')
localStorage.removeItem('refreshToken')
```

如果有用户 store，也要清空 store。

---

## 十四、建议提交

```bash
git add frontend/src/api frontend/src/views frontend/src/router
git commit -m "feat: add cancel account deletion frontend flow"
```

---

## 十五、本轮前端验收标准

完成后应满足：

```text
1. 登录页有“取消注销”入口。
2. 访问 /cancel-delete 不需要登录。
3. 取消注销页可以发送邮箱验证码。
4. 取消注销页可以提交验证码恢复账号。
5. 注销成功后前端清除本地 token。
6. 注销成功后跳转到取消注销页面或登录页。
7. 忘记密码页提示注销冷静期用户先取消注销。
8. 待注销账号取消注销后可以重新登录。
```
