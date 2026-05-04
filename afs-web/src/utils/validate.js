export const validateUsername = (username) => {
  if (!username) return '请输入用户名'
  if (username.length < 3) return '用户名长度不能少于3位'
  if (username.length > 20) return '用户名长度不能超过20位'
  if (!/^[a-zA-Z0-9_]+$/.test(username)) {
    return '用户名只能包含字母、数字和下划线'
  }
  return ''
}

export const validatePassword = (password) => {
  if (!password) return '请输入密码'
  if (password.length < 6) return '密码长度不能少于6位'
  if (password.length > 20) return '密码长度不能超过20位'
  return ''
}

export const validateEmail = (email) => {
  if (!email) return '请输入邮箱'
  const reg = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!reg.test(email)) return '请输入有效的邮箱地址'
  return ''
}

export const validatePhone = (phone) => {
  if (!phone) return '请输入手机号'
  const reg = /^1[3-9]\d{9}$/
  if (!reg.test(phone)) return '请输入有效的手机号'
  return ''
}
