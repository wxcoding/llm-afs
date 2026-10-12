import request from '@/utils/request'

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
