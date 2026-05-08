<template>
  <div class="auth-page"><div class="card auth-card">
    <h2>注册账号</h2>
    <form @submit.prevent="handleRegister">
      <div class="form-item"><label>用户名</label><input class="input" v-model="form.username" placeholder="3~20 位用户名" /></div>
      <div class="form-item"><label>邮箱</label><div class="inline"><input class="input" v-model="form.email" placeholder="name@example.com" /><button class="btn btn-default" type="button" @click="sendCode" :disabled="codeLoading">{{ codeLoading ? '发送中' : '发验证码' }}</button></div></div>
      <div class="form-item"><label>邮箱验证码</label><input class="input" v-model="form.emailCode" placeholder="请输入 6 位验证码" /></div>
      <div class="form-item"><label>密码</label><input class="input" type="password" v-model="form.password" placeholder="至少 6 位" /></div>
      <div class="form-item"><label>昵称</label><input class="input" v-model="form.nickname" placeholder="可选" /></div>
      <button class="btn btn-primary submit-btn" :disabled="loading">{{ loading ? '注册中...' : '立即注册' }}</button>
    </form>
    <p class="tip-text">已有账号？<span @click="$router.push('/login')">去登录</span></p>
  </div></div>
</template>
<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { registerApi, sendRegisterEmailCodeApi } from '@/api/auth'
const router = useRouter(); const loading=ref(false); const codeLoading=ref(false)
const form=reactive({username:'',email:'',emailCode:'',password:'',nickname:''})
async function sendCode(){ if(!form.email){alert('请先输入邮箱');return} codeLoading.value=true; try{ const res=await sendRegisterEmailCodeApi({email:form.email}); alert(res.data.message || '验证码已发送，请查收邮箱') } finally{ codeLoading.value=false } }
async function handleRegister(){ if(!form.username||!form.email||!form.emailCode||!form.password){alert('请填写完整注册信息');return} loading.value=true; try{ const res=await registerApi(form); alert(res.message||'注册成功'); router.push('/login') } finally{ loading.value=false } }
</script>
<style scoped>.auth-page{min-height:100vh;display:flex;align-items:center;justify-content:center;padding:20px}.auth-card{width:460px;padding:28px}h2{font-size:28px;margin-bottom:22px}.form-item{margin-bottom:15px}label{display:block;margin-bottom:6px;color:#9eb1cb}.inline{display:flex;gap:10px}.inline .btn{width:110px}.submit-btn{width:100%}.tip-text{text-align:center;margin-top:16px}.tip-text span{color:#7db9ff;cursor:pointer}</style>
