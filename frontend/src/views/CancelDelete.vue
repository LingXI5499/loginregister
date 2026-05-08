<template>
  <div class="auth-page"><div class="card auth-card">
    <h2>取消账号注销</h2>
    <p class="warn">输入注册邮箱，验证通过后即可取消账号注销</p>
    <div class="form-item"><label>邮箱</label><div class="inline"><input class="input" v-model="form.email" placeholder="请输入注册邮箱" /><button class="btn btn-default" @click="handleSendCode" :disabled="codeSending || countdown > 0">{{ codeSending ? '发送中' : countdown > 0 ? `${countdown}s` : '发送验证码' }}</button></div></div>
    <div class="form-item"><label>验证码</label><input class="input" v-model="form.emailCode" placeholder="请输入6位验证码" maxlength="6" /></div>
    <button class="btn btn-primary submit-btn" @click="handleCancel" :disabled="submitting">{{ submitting ? '提交中...' : '取消注销' }}</button>
    <p class="tip-text"><span @click="$router.push('/login')">返回登录</span></p>
  </div></div>
</template>
<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { sendCancelDeleteCodeApi, cancelDeleteApi } from '@/api/auth'
const router=useRouter(); const submitting=ref(false); const codeSending=ref(false); const countdown=ref(0)
const form=reactive({email:'',emailCode:''})
async function handleSendCode(){
  if(!form.email){alert('请先输入邮箱');return}
  codeSending.value=true
  try{ const res=await sendCancelDeleteCodeApi({email:form.email}); alert(res.data.message||'验证码已发送（如账号处于注销冷静期）'); countdown.value=60; const t=setInterval(()=>{countdown.value--;if(countdown.value<=0)clearInterval(t)},1000) }catch(e){alert(e.message||'发送失败')}finally{codeSending.value=false}
}
async function handleCancel(){
  if(!form.email||!form.emailCode){alert('请填写完整信息');return}
  submitting.value=true
  try{ const res=await cancelDeleteApi({email:form.email,emailCode:form.emailCode}); alert(res.message||'账号注销已取消，请重新登录'); router.push('/login') }catch(e){alert(e.message||'取消失败')}finally{submitting.value=false}
}
</script>
<style scoped>.auth-page{min-height:100vh;display:flex;align-items:center;justify-content:center;padding:20px}.auth-card{width:440px;padding:28px}h2{font-size:28px;margin-bottom:14px}.warn{line-height:1.7;color:#fbbf24;background:rgba(245,158,11,.1);padding:10px;border-radius:12px;margin-bottom:16px}.form-item{margin-bottom:16px}label{display:block;margin-bottom:6px;color:#9eb1cb}.inline{display:flex;gap:10px}.inline .btn{width:110px}.submit-btn{width:100%}.tip-text{text-align:center;margin-top:16px}.tip-text span{color:#7db9ff;cursor:pointer}</style>
