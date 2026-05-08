<template>
  <div class="profile-page"><div class="card profile-card">
    <h2>个人中心</h2>
    <div v-if="loading" class="muted">正在加载...</div>
    <div v-else-if="user" class="info-list">
      <p><strong>ID：</strong>{{ user.id }}</p>
      <p><strong>用户名：</strong>{{ user.username }}</p>
      <p><strong>邮箱：</strong>{{ user.email }} <span class="badge badge-success" v-if="user.emailVerified===1">已验证</span></p>
      <p><strong>昵称：</strong>{{ user.nickname || '-' }}</p>
      <p><strong>头像：</strong><span class="muted">{{ user.avatarUrl || '未设置' }}</span></p>
      <p><strong>状态：</strong>{{ user.status }}</p>
    </div>

    <div class="divider"></div>
    <h3>修改资料</h3>
    <div class="form-grid two">
      <input class="input" v-model="profileForm.nickname" placeholder="昵称，最多 50 个字符" />
      <input class="input" v-model="profileForm.avatarUrl" placeholder="头像 URL，最多 255 个字符" />
    </div>
    <button class="btn btn-primary section-btn" @click="saveProfile" :disabled="profileLoading">{{ profileLoading ? '保存中...' : '保存资料' }}</button>

    <div class="divider"></div>
    <h3>修改密码</h3>
    <div class="form-grid three">
      <input class="input" type="password" v-model="pwd.oldPassword" placeholder="旧密码" />
      <input class="input" type="password" v-model="pwd.newPassword" placeholder="新密码，6~64 位" />
      <input class="input" type="password" v-model="pwd.confirmPassword" placeholder="确认新密码" />
    </div>
    <button class="btn btn-primary section-btn" @click="changePwd">修改密码并重新登录</button>

    <div class="divider"></div>
    <h3>换绑邮箱</h3>
    <p class="muted helper">换绑成功后会撤销全部登录会话，需要用新邮箱或用户名重新登录。</p>
    <div class="form-grid email-change">
      <input class="input" v-model="emailForm.newEmail" placeholder="新邮箱" />
      <button class="btn btn-default" @click="sendChangeEmailCode" :disabled="emailCodeLoading || countdown.running.value">{{ emailCodeLoading ? '发送中' : countdown.text.value }}</button>
      <input class="input" v-model="emailForm.emailCode" placeholder="验证码" maxlength="6" />
      <input class="input" type="password" v-model="emailForm.currentPassword" placeholder="当前密码" />
    </div>
    <button class="btn btn-primary section-btn" @click="confirmChangeEmail" :disabled="emailChanging">{{ emailChanging ? '换绑中...' : '确认换绑邮箱' }}</button>

    <div class="actions"><button class="btn btn-default" @click="$router.push('/')">首页</button><button class="btn btn-default" @click="$router.push('/devices')">设备管理</button><button class="btn btn-default" @click="$router.push('/delete-account')">注销账号</button><button class="btn btn-danger" @click="logout">退出登录</button><button class="btn btn-danger" @click="logoutAll">退出全部设备</button></div>
  </div></div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { changePasswordApi, confirmChangeEmailApi, getCurrentUserApi, logoutAllApi, logoutApi, sendChangeEmailCodeApi, updateProfileApi } from '@/api/auth'
import { removeAuthTokens } from '@/utils/auth'
import { useCountdown } from '@/utils/useCountdown'
import { isValidEmail, isValidEmailCode, isValidPassword, trimForm } from '@/utils/validators'

const router=useRouter(); const loading=ref(false); const user=ref(null); const profileLoading=ref(false); const emailCodeLoading=ref(false); const emailChanging=ref(false); const countdown=useCountdown(60)
const profileForm=reactive({nickname:'',avatarUrl:''})
const pwd=reactive({oldPassword:'',newPassword:'',confirmPassword:''})
const emailForm=reactive({newEmail:'',emailCode:'',currentPassword:''})

async function loadUser(){ loading.value=true; try{ const res=await getCurrentUserApi(); user.value=res.data; profileForm.nickname=res.data.nickname||''; profileForm.avatarUrl=res.data.avatarUrl||'' } finally{ loading.value=false } }
async function saveProfile(){ trimForm(profileForm); profileLoading.value=true; try{ const res=await updateProfileApi(profileForm); alert(res.message||'资料修改成功'); await loadUser() } finally{ profileLoading.value=false } }
async function logout(){ try{ await logoutApi() } catch(e){} removeAuthTokens(); router.push('/login') }
async function logoutAll(){ try{ await logoutAllApi() } catch(e){} removeAuthTokens(); router.push('/login') }
async function changePwd(){ trimForm(pwd); if(!pwd.oldPassword||!pwd.newPassword){alert('请填写旧密码和新密码');return} if(!isValidPassword(pwd.newPassword)){alert('新密码长度必须在 6~64 位之间');return} if(pwd.newPassword!==pwd.confirmPassword){alert('两次输入的新密码不一致');return} const res=await changePasswordApi({oldPassword:pwd.oldPassword,newPassword:pwd.newPassword}); alert(res.message||'密码已修改，请重新登录'); removeAuthTokens(); router.push('/login') }
async function sendChangeEmailCode(){ trimForm(emailForm); if(!isValidEmail(emailForm.newEmail)){alert('请输入正确的新邮箱地址');return} emailCodeLoading.value=true; try{ const res=await sendChangeEmailCodeApi({newEmail:emailForm.newEmail}); alert(res.data.message||'验证码已发送，请查收邮箱'); countdown.start() } finally{ emailCodeLoading.value=false } }
async function confirmChangeEmail(){ trimForm(emailForm); if(!isValidEmail(emailForm.newEmail)){alert('请输入正确的新邮箱地址');return} if(!isValidEmailCode(emailForm.emailCode)){alert('验证码必须是 6 位数字');return} if(!emailForm.currentPassword){alert('请输入当前密码');return} emailChanging.value=true; try{ const res=await confirmChangeEmailApi(emailForm); alert(res.message||'邮箱换绑成功，请重新登录'); removeAuthTokens(); router.push('/login') } finally{ emailChanging.value=false } }
onMounted(loadUser)
</script>

<style scoped>
.profile-page{min-height:100vh;display:flex;align-items:center;justify-content:center;padding:20px}.profile-card{width:min(880px,100%);padding:28px}h2{font-size:28px;margin-bottom:18px}h3{margin:18px 0 12px}.info-list p{margin-bottom:10px;color:var(--text-2)}.divider{height:1px;background:var(--line-1);margin:22px 0}.helper{margin-bottom:12px}.form-grid{display:grid;gap:10px}.form-grid.two{grid-template-columns:1fr 1fr}.form-grid.three{grid-template-columns:1fr 1fr 1fr}.form-grid.email-change{grid-template-columns:1fr 130px 1fr 1fr}.section-btn{margin-top:12px}.actions{display:flex;flex-wrap:wrap;gap:10px;margin-top:28px}@media(max-width:800px){.form-grid.two,.form-grid.three,.form-grid.email-change{grid-template-columns:1fr}.actions .btn{width:100%}}
</style>