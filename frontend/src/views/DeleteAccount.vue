<template>
  <div class="auth-page"><div class="card auth-card">
    <h2>注销账号</h2><p class="warn">账号会进入待注销状态，并立即撤销全部登录会话。冷静期内账号无法继续登录。</p>
    <button class="btn btn-default" @click="sendCode" :disabled="codeLoading || countdown.running.value">{{ codeLoading ? '发送中...' : countdown.text.value }}</button>
    <div class="form-item"><label>邮箱验证码</label><input class="input" v-model="form.emailCode" placeholder="请输入 6 位验证码" maxlength="6" /></div>
    <div class="form-item"><label>注销原因</label><textarea class="textarea" v-model="form.reason" placeholder="可选"></textarea></div>
    <button class="btn btn-danger submit-btn" @click="deleteAccount" :disabled="loading">确认注销</button>
    <p class="tip-text"><span @click="$router.push('/profile')">返回个人中心</span></p>
  </div></div>
</template>
<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { sendDeleteCodeApi, requestDeleteAccountApi } from '@/api/auth'
import { removeAuthTokens } from '@/utils/auth'
import { useCountdown } from '@/utils/useCountdown'
import { isValidEmailCode, trimForm } from '@/utils/validators'
const router=useRouter(); const loading=ref(false); const codeLoading=ref(false); const countdown=useCountdown(60); const form=reactive({emailCode:'',reason:''})
async function sendCode(){ codeLoading.value=true; try{ const res=await sendDeleteCodeApi(); alert(res.data.message||'验证码已发送，请查收邮箱'); countdown.start() } finally{ codeLoading.value=false } }
async function deleteAccount(){ trimForm(form); if(!isValidEmailCode(form.emailCode)){alert('请输入 6 位邮箱验证码');return} if(!confirm('确认注销账号？该操作会退出全部设备。')) return; loading.value=true; try{ const res=await requestDeleteAccountApi(form); alert(res.message||'账号已进入待注销状态'); removeAuthTokens(); router.push('/cancel-delete') } finally{ loading.value=false } }
</script>
<style scoped>.auth-page{min-height:100vh;display:flex;align-items:center;justify-content:center;padding:20px}.auth-card{width:480px;padding:28px}h2{font-size:28px;margin-bottom:14px}.warn{line-height:1.7;color:#fbbf24;background:rgba(245,158,11,.1);padding:10px;border-radius:12px;margin-bottom:16px}.form-item{margin:16px 0}label{display:block;margin-bottom:6px;color:#9eb1cb}.submit-btn{width:100%}.tip-text{text-align:center;margin-top:16px}.tip-text span{color:#7db9ff;cursor:pointer}</style>
