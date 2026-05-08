import axios from 'axios'
import router from '@/router'
import { getAccessToken, getRefreshToken, setAuthTokens, removeAuthTokens } from './auth'

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'

const request = axios.create({ baseURL, timeout: 15000 })

let refreshing = false
let waitQueue = []

function redirectLogin(message) {
  removeAuthTokens()
  if (message) alert(message)
  if (router.currentRoute.value.path !== '/login') router.push('/login')
}

function flushQueue(error, token) {
  waitQueue.forEach(({ resolve, reject }) => error ? reject(error) : resolve(token))
  waitQueue = []
}

async function refreshAccessToken() {
  const refreshToken = getRefreshToken()
  if (!refreshToken) throw new Error('no refresh token')
  const resp = await axios.post(`${baseURL}/auth/token/refresh`, { refreshToken })
  const res = resp.data
  if (res.code !== 200) throw new Error(res.message || 'refresh failed')
  setAuthTokens(res.data.accessToken, res.data.refreshToken, res.data.username)
  return res.data.accessToken
}

request.interceptors.request.use(config => {
  const token = getAccessToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 401) return Promise.reject({ response, config: response.config })
    if (res.code !== 200) {
      alert(res.message || '请求失败')
      return Promise.reject(res)
    }
    return res
  },
  async error => {
    const original = error.config || error.response?.config
    const status = error.response?.status
    const data = error.response?.data
    if (status === 401 && original && !original._retry && !original.url.includes('/auth/token/refresh')) {
      original._retry = true
      if (refreshing) {
        try {
          const token = await new Promise((resolve, reject) => waitQueue.push({ resolve, reject }))
          original.headers.Authorization = `Bearer ${token}`
          return request(original)
        } catch (e) {
          redirectLogin('登录状态已失效，请重新登录')
          return Promise.reject(e)
        }
      }
      refreshing = true
      try {
        const token = await refreshAccessToken()
        flushQueue(null, token)
        original.headers.Authorization = `Bearer ${token}`
        return request(original)
      } catch (e) {
        flushQueue(e)
        redirectLogin(data?.message || '登录状态已失效，请重新登录')
        return Promise.reject(e)
      } finally {
        refreshing = false
      }
    }
    if (status === 401) redirectLogin(data?.message || '登录状态已失效，请重新登录')
    else alert(data?.message || '网络异常，请确认后端 http://localhost:7070 已启动')
    return Promise.reject(error)
  }
)

export default request
