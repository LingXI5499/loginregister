<template>
  <div class="auth-page"><div class="card auth-card">
    <h2>登录账号</h2>
    <div class="tabs"><button :class="['tab',{active:mode==='password'}]" @click="mode='password'">账号密码</button><button :class="['tab',{active:mode==='email'}]" @click="mode='email'">邮箱验证码</button></div>
    <form v-if="mode==='password'" @submit.prevent="handlePasswordLogin">
      <div class="form-item"><label>用户名/邮箱</label><input class="input" v-model="passwordForm.account" placeholder="请输入用户名或邮箱" /></div>
      <div class="form-item"><label>密码</label><input class="input" type="password" v-model="passwordForm.password" placeholder="请输入密码" /></div>
      <button class="btn btn-primary submit-btn" :disabled="loading">{{ loading ? '登录中...' : '立即登录' }}</button>
    </form>
    <form v-else @submit.prevent="handleEmailLogin">
      <div class="form-item"><label>邮箱</label><div class="inline"><input class="input" v-model="emailForm.email" placeholder="name@example.com" /><button class="btn btn-default" type="button" @click="sendEmailCode" :disabled="codeLoading">{{ codeLoading ? '发送中' : '发验证码' }}</button></div></div>
      <div class="form-item"><label>验证码</label><input class="input" v-model="emailForm.code" placeholder="请输入 6 位验证码" /></div>
      <button class="btn btn-primary submit-btn" :disabled="loading">{{ loading ? '登录中...' : '验证码登录' }}</button>
    </form>
    <p class="tip-text"><span @click="$router.push('/forgot-password')">忘记密码</span> · <span @click="$router.push('/cancel-delete')">取消注销</span> · 没有账号？<span @click="$router.push('/register')">去注册</span></p>
  </div></div>
</template>
<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { loginPasswordApi, sendEmailLoginCodeApi, loginEmailCodeApi } from '@/api/auth'
import { setAuthTokens } from '@/utils/auth'
const router=useRouter(); const mode=ref('password'); const loading=ref(false); const codeLoading=ref(false)
const passwordForm=reactive({account:'',password:'',deviceName:navigator.userAgent.slice(0,80)})
const emailForm=reactive({email:'',code:'',deviceName:navigator.userAgent.slice(0,80)})
function saveLogin(data){ setAuthTokens(data.accessToken,data.refreshToken,data.username); alert('登录成功'); router.push('/profile') }
async function handlePasswordLogin(){ if(!passwordForm.account||!passwordForm.password){alert('请输入账号和密码');return} loading.value=true; try{ const res=await loginPasswordApi(passwordForm); saveLogin(res.data) } finally{ loading.value=false } }
async function sendEmailCode(){ if(!emailForm.email){alert('请先输入邮箱');return} codeLoading.value=true; try{ const res=await sendEmailLoginCodeApi({email:emailForm.email}); alert(res.data.message||'验证码已发送，请查收邮箱') } finally{ codeLoading.value=false } }
async function handleEmailLogin(){ if(!emailForm.email||!emailForm.code){alert('请输入邮箱和验证码');return} loading.value=true; try{ const res=await loginEmailCodeApi(emailForm); saveLogin(res.data) } finally{ loading.value=false } }
</script>
<style scoped>.auth-page{min-height:100vh;display:flex;align-items:center;justify-content:center;padding:20px}.auth-card{width:440px;padding:28px}h2{font-size:28px;margin-bottom:18px}.tabs{display:flex;gap:10px;margin-bottom:20px}.tab{flex:1;height:36px;border-radius:12px;border:1px solid var(--line-1);background:rgba(10,16,32,.9);color:var(--text-2);cursor:pointer}.tab.active{border-color:rgba(77,163,255,.7);color:#fff;background:rgba(77,163,255,.2)}.form-item{margin-bottom:16px}label{display:block;margin-bottom:6px;color:#9eb1cb}.inline{display:flex;gap:10px}.inline .btn{width:110px}.submit-btn{width:100%}.tip-text{text-align:center;margin-top:16px}.tip-text span{color:#7db9ff;cursor:pointer}</style>
