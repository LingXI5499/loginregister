<template>
  <div class="profile-page"><div class="card profile-card">
    <h2>个人中心</h2>
    <div v-if="loading" class="muted">正在加载...</div>
    <div v-else-if="user" class="info-list">
      <p><strong>ID：</strong>{{ user.id }}</p><p><strong>用户名：</strong>{{ user.username }}</p><p><strong>邮箱：</strong>{{ user.email }} <span class="badge badge-success" v-if="user.emailVerified===1">已验证</span></p><p><strong>昵称：</strong>{{ user.nickname }}</p><p><strong>状态：</strong>{{ user.status }}</p>
    </div>
    <div class="divider"></div>
    <h3>修改密码</h3>
    <div class="form-row"><input class="input" type="password" v-model="pwd.oldPassword" placeholder="旧密码" /><input class="input" type="password" v-model="pwd.newPassword" placeholder="新密码" /><button class="btn btn-primary" @click="changePwd">修改</button></div>
    <div class="actions"><button class="btn btn-default" @click="$router.push('/')">首页</button><button class="btn btn-default" @click="$router.push('/devices')">设备管理</button><button class="btn btn-default" @click="$router.push('/delete-account')">注销账号</button><button class="btn btn-danger" @click="logout">退出登录</button><button class="btn btn-danger" @click="logoutAll">退出全部设备</button></div>
  </div></div>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getCurrentUserApi, logoutApi, logoutAllApi, changePasswordApi } from '@/api/auth'
import { removeAuthTokens } from '@/utils/auth'
const router=useRouter(); const loading=ref(false); const user=ref(null); const pwd=reactive({oldPassword:'',newPassword:''})
async function loadUser(){ loading.value=true; try{ const res=await getCurrentUserApi(); user.value=res.data } finally{ loading.value=false } }
async function logout(){ try{ await logoutApi() } catch(e){} removeAuthTokens(); router.push('/login') }
async function logoutAll(){ try{ await logoutAllApi() } catch(e){} removeAuthTokens(); router.push('/login') }
async function changePwd(){ if(!pwd.oldPassword||!pwd.newPassword){alert('请填写旧密码和新密码');return} const res=await changePasswordApi(pwd); alert(res.message||'密码已修改'); removeAuthTokens(); router.push('/login') }
onMounted(loadUser)
</script>
<style scoped>.profile-page{min-height:100vh;display:flex;align-items:center;justify-content:center;padding:20px}.profile-card{width:min(760px,100%);padding:28px}h2{font-size:28px;margin-bottom:18px}h3{margin:18px 0 12px}.info-list p{margin-bottom:10px;color:var(--text-2)}.divider{height:1px;background:var(--line-1);margin:22px 0}.form-row{display:grid;grid-template-columns:1fr 1fr auto;gap:10px}.actions{display:flex;flex-wrap:wrap;gap:10px;margin-top:22px}@media(max-width:700px){.form-row{grid-template-columns:1fr}.actions .btn{width:100%}}</style>
