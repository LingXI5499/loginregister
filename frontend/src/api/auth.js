import request from '@/utils/request'

export const sendRegisterEmailCodeApi = data => request({ url: '/auth/email-code/send', method: 'post', data })
export const registerApi = data => request({ url: '/auth/register', method: 'post', data })
export const loginPasswordApi = data => request({ url: '/auth/login/password', method: 'post', data })
export const loginApi = loginPasswordApi
export const sendEmailLoginCodeApi = data => request({ url: '/auth/login/email-code/send', method: 'post', data })
export const loginEmailCodeApi = data => request({ url: '/auth/login/email-code/verify', method: 'post', data })
export const logoutApi = () => request({ url: '/auth/logout', method: 'post' })
export const logoutAllApi = () => request({ url: '/auth/logout-all', method: 'post' })
export const getCurrentUserApi = () => request({ url: '/user/me', method: 'get' })
export const resetPasswordRequestApi = data => request({ url: '/auth/password/reset/request', method: 'post', data })
export const resetPasswordConfirmApi = data => request({ url: '/auth/password/reset/confirm', method: 'post', data })
export const changePasswordApi = data => request({ url: '/auth/password/change', method: 'post', data })
export const getSessionsApi = () => request({ url: '/auth/sessions', method: 'get' })
export const revokeSessionApi = sessionId => request({ url: `/auth/sessions/${sessionId}`, method: 'delete' })
export const sendDeleteCodeApi = () => request({ url: '/account/delete/code/send', method: 'post' })
export const requestDeleteAccountApi = data => request({ url: '/account/delete/request', method: 'post', data })
