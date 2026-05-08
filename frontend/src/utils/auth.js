const ACCESS_TOKEN_KEY = 'SMARTBLOG_ACCESS_TOKEN'
const REFRESH_TOKEN_KEY = 'SMARTBLOG_REFRESH_TOKEN'
const USERNAME_KEY = 'SMARTBLOG_USERNAME'

export function getAccessToken() { return localStorage.getItem(ACCESS_TOKEN_KEY) }
export function getRefreshToken() { return localStorage.getItem(REFRESH_TOKEN_KEY) }
export function setAuthTokens(accessToken, refreshToken, username) {
  if (accessToken) localStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
  if (refreshToken) localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
  if (username) localStorage.setItem(USERNAME_KEY, username)
}
export function removeAuthTokens() {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.removeItem(USERNAME_KEY)
}
export const getToken = getAccessToken
export const setToken = token => setAuthTokens(token, null, null)
export const removeToken = removeAuthTokens
