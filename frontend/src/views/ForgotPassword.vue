<template>
  <div class="auth-page"><div class="card auth-card">
    <h2>找回密码</h2>
    <div class="form-item"><label>邮箱</label><div class="inline"><input class="input" v-model="form.email" placeholder="请输入已验证邮箱" /><button class="btn btn-default" @click="sendCode" :disabled="codeLoading">{{ codeLoading ? '发送中' : '发验证码' }}</button></div></div>
    <div class="form-item"><label>验证码</label><input class="input" v-model="form.code" placeholder="请输入验证码" /></div>
    <div class="form-item"><label>新密码</label><input class="input" type="password" v-model="form.newPassword" placeholder="至少 6 位" /></div>
    <button class="btn btn-primary submit-btn" @click="resetPassword" :disabled="loading">{{ loading ? '提交中...' : '重置密码' }}</button>
    <p class="tip-text"><span @click="$router.push('/login')">返回登录</span> · <span @click="$router.push('/cancel-delete')">取消注销</span></p>
  </div></div>
</template>
<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { resetPasswordRequestApi, resetPasswordConfirmApi } from '@/api/auth'
const router=useRouter(); const loading=ref(false); const codeLoading=ref(false)
const form=reactive({email:'',code:'',newPassword:''})
async function sendCode(){ if(!form.email){alert('请先输入邮箱');return} codeLoading.value=true; try{ const res=await resetPasswordRequestApi({email:form.email}); alert(res.data.message||res.message) } finally{ codeLoading.value=false } }
async function resetPassword(){ if(!form.email||!form.code||!form.newPassword){alert('请填写完整信息');return} loading.value=true; try{ const res=await resetPasswordConfirmApi(form); alert(res.message||'密码已重置'); router.push('/login') } finally{ loading.value=false } }
</script>
<style scoped>.auth-page{min-height:100vh;display:flex;align-items:center;justify-content:center;padding:20px}.auth-card{width:440px;padding:28px}h2{font-size:28px;margin-bottom:22px}.form-item{margin-bottom:16px}label{display:block;margin-bottom:6px;color:#9eb1cb}.inline{display:flex;gap:10px}.inline .btn{width:110px}.submit-btn{width:100%}.tip-text{text-align:center;margin-top:16px}.tip-text span{color:#7db9ff;cursor:pointer}</style>
