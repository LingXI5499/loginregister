export const usernamePattern = /^[A-Za-z0-9_]{3,20}$/
export const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
export const emailCodePattern = /^\d{6}$/

export function isValidUsername(value) {
  return usernamePattern.test((value || '').trim())
}

export function isValidEmail(value) {
  return emailPattern.test((value || '').trim())
}

export function isValidEmailCode(value) {
  return emailCodePattern.test((value || '').trim())
}

export function isValidPassword(value) {
  const text = value || ''
  return text.length >= 6 && text.length <= 64
}

export function trimForm(form) {
  Object.keys(form).forEach(key => {
    if (typeof form[key] === 'string') form[key] = form[key].trim()
  })
}
