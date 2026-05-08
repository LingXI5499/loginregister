<template>
  <div class="auth-page">
    <div class="card auth-card">
      <h2>注册账号</h2>
      <form @submit.prevent="handleRegister">
        <div class="form-item">
          <label>用户名</label>
          <input class="input" v-model="form.username" placeholder="3~20 位，只能字母、数字、下划线" />
        </div>
        <div class="form-item">
          <label>昵称</label>
          <input class="input" v-model="form.nickname" placeholder="可选，最多 50 个字符" />
        </div>
        <div class="form-item">
          <label>邮箱</label>
          <div class="inline">
            <input class="input" v-model="form.email" placeholder="name@example.com" />
            <button class="btn btn-default" type="button" @click="sendCode" :disabled="codeLoading || countdown.running.value">
              {{ codeLoading ? '发送中' : countdown.text.value }}
            </button>
          </div>
        </div>
        <div class="form-item">
          <label>邮箱验证码</label>
          <input class="input" v-model="form.emailCode" placeholder="请输入 6 位验证码" maxlength="6" />
        </div>
        <div class="form-item">
          <label>密码</label>
          <input class="input" type="password" v-model="form.password" placeholder="6~64 位" />
        </div>
        <div class="form-item">
          <label>确认密码</label>
          <input class="input" type="password" v-model="form.confirmPassword" placeholder="再次输入密码" />
        </div>
        <button class="btn btn-primary submit-btn" :disabled="loading">{{ loading ? '注册中...' : '立即注册' }}</button>
      </form>
      <p class="tip-text">已有账号？<span @click="$router.push('/login')">去登录</span></p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { registerApi, sendRegisterEmailCodeApi } from '@/api/auth'
import { useCountdown } from '@/utils/useCountdown'
import { isValidEmail, isValidEmailCode, isValidPassword, isValidUsername, trimForm } from '@/utils/validators'

const router = useRouter()
const loading = ref(false)
const codeLoading = ref(false)
const countdown = useCountdown(60)
const form = reactive({ username: '', nickname: '', email: '', emailCode: '', password: '', confirmPassword: '' })

async function sendCode() {
  trimForm(form)
  if (!isValidEmail(form.email)) { alert('请输入正确的邮箱地址'); return }
  codeLoading.value = true
  try {
    const res = await sendRegisterEmailCodeApi({ email: form.email })
    alert(res.data.message || '验证码已发送，请查收邮箱')
    countdown.start()
  } finally {
    codeLoading.value = false
  }
}

async function handleRegister() {
  trimForm(form)
  if (!isValidUsername(form.username)) { alert('用户名只能包含字母、数字和下划线，长度 3~20 位'); return }
  if (!isValidEmail(form.email)) { alert('请输入正确的邮箱地址'); return }
  if (!isValidEmailCode(form.emailCode)) { alert('验证码必须是 6 位数字'); return }
  if (!isValidPassword(form.password)) { alert('密码长度必须在 6~64 位之间'); return }
  if (form.password !== form.confirmPassword) { alert('两次输入的密码不一致'); return }
  loading.value = true
  try {
    const { confirmPassword, ...payload } = form
    const res = await registerApi(payload)
    alert(res.message || '注册成功')
    router.push('/login')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page{min-height:100vh;display:flex;align-items:center;justify-content:center;padding:20px}.auth-card{width:460px;padding:28px}h2{font-size:28px;margin-bottom:22px}.form-item{margin-bottom:15px}label{display:block;margin-bottom:6px;color:#9eb1cb}.inline{display:flex;gap:10px}.inline .btn{width:130px}.submit-btn{width:100%}.tip-text{text-align:center;margin-top:16px}.tip-text span{color:#7db9ff;cursor:pointer}
</style>
