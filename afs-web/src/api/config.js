import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.response.use(
  response => response.data,
  error => Promise.reject(error)
)

export const getAllConfigs = () => {
  return request.get('/config/all')
}

export const getConfigValue = (configKey) => {
  return request.get(`/config/${configKey}`)
}

export const getConfigsByType = (configType) => {
  return request.get(`/config/type/${configType}`)
}

export const updateConfig = (configKey, configValue) => {
  return request.put(`/config/${configKey}`, null, { params: { configValue } })
}

export const initDefaultConfigs = () => {
  return request.post('/config/init')
}
