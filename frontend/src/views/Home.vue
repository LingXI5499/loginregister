<template>
  <div class="home-page">
    <div class="card home-card">
      <h1>SmartBlog 账号认证系统</h1>
      <p>当前版本支持用户名/邮箱注册、账号密码登录、邮箱验证码登录、忘记密码、会话续期、服务端退出、设备管理、修改密码和注销流程基础版。</p>
      <div class="actions">
        <button class="btn btn-primary" @click="$router.push('/login')">登录</button>
        <button class="btn btn-default" @click="$router.push('/register')">注册</button>
        <button class="btn btn-default" @click="$router.push('/profile')">个人中心</button>
        <button class="btn btn-default" @click="$router.push('/devices')">设备管理</button>
      </div>
    </div>
  </div>
</template>
<style scoped>
.home-page{min-height:100vh;display:flex;align-items:center;justify-content:center;padding:20px}.home-card{width:min(760px,100%);padding:34px}h1{font-size:32px;margin-bottom:16px}p{line-height:1.9;color:var(--text-2)}.actions{display:flex;flex-wrap:wrap;gap:12px;margin-top:24px}
</style>
