import { createRouter, createWebHistory } from 'vue-router'
import { getAccessToken } from '@/utils/auth'
import Home from '@/views/Home.vue'
import Login from '@/views/Login.vue'
import Register from '@/views/Register.vue'
import Profile from '@/views/Profile.vue'
import ForgotPassword from '@/views/ForgotPassword.vue'
import DeviceManage from '@/views/DeviceManage.vue'
import DeleteAccount from '@/views/DeleteAccount.vue'

const routes = [
  { path: '/', name: 'Home', component: Home },
  { path: '/login', name: 'Login', component: Login },
  { path: '/register', name: 'Register', component: Register },
  { path: '/forgot-password', name: 'ForgotPassword', component: ForgotPassword },
  { path: '/profile', name: 'Profile', component: Profile, meta: { requiresAuth: true } },
  { path: '/devices', name: 'DeviceManage', component: DeviceManage, meta: { requiresAuth: true } },
  { path: '/delete-account', name: 'DeleteAccount', component: DeleteAccount, meta: { requiresAuth: true } }
]

const router = createRouter({ history: createWebHistory(), routes })
router.beforeEach((to, from, next) => {
  if (to.meta.requiresAuth && !getAccessToken()) next('/login')
  else next()
})
export default router
