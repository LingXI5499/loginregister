<template>
  <div class="page"><div class="card panel"><h2>设备管理</h2><p class="muted">查看当前账号有效会话，并可踢下线指定设备。</p><table class="table"><thead><tr><th>设备</th><th>IP</th><th>登录时间</th><th>过期时间</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-for="s in sessions" :key="s.sessionId"><td>{{ s.deviceName }}</td><td>{{ s.ip }}</td><td>{{ s.createTime }}</td><td>{{ s.expireTime }}</td><td><span :class="['badge', s.current ? 'badge-success' : 'badge-muted']">{{ s.current ? '当前设备' : '其他设备' }}</span></td><td><button class="btn btn-danger" :disabled="s.current" @click="revoke(s.sessionId)">踢下线</button></td></tr></tbody></table><div class="actions"><button class="btn btn-default" @click="$router.push('/profile')">返回个人中心</button><button class="btn btn-primary" @click="load">刷新</button></div></div></div>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { getSessionsApi, revokeSessionApi } from '@/api/auth'
const sessions=ref([])
async function load(){ const res=await getSessionsApi(); sessions.value=res.data||[] }
async function revoke(id){ if(!confirm('确认踢下线该设备？')) return; await revokeSessionApi(id); await load() }
onMounted(load)
</script>
<style scoped>.page{min-height:100vh;padding:28px}.panel{padding:28px;max-width:1100px;margin:0 auto}h2{font-size:28px;margin-bottom:10px}.table{margin-top:18px}.actions{display:flex;gap:10px;margin-top:18px}</style>
