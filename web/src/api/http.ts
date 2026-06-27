import axios, { AxiosError } from 'axios'
import { ElMessage } from 'element-plus'

import router from '@/router'
import { useAuthStore } from '@/stores/auth'
import type { ApiResult } from '@/types/api'

export const http = axios.create({
  baseURL: '/api',
  timeout: 10000
})

http.interceptors.request.use((config) => {
  const authStore = useAuthStore()
  if (authStore.tokenValue) {
    config.headers.Authorization = `${authStore.tokenPrefix} ${authStore.tokenValue}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const result = response.data as ApiResult<unknown>
    if (typeof result?.code === 'number' && result.code !== 0) {
      return Promise.reject(new Error(result.message || '请求处理失败'))
    }
    return response
  },
  async (error: AxiosError<ApiResult<unknown>>) => {
    if (error.response?.status === 401) {
      const authStore = useAuthStore()
      authStore.clearSession()
      await router.replace('/login')
      ElMessage.warning('登录状态已失效，请重新登录')
    }
    return Promise.reject(error)
  }
)

export async function unwrap<T>(request: Promise<{ data: ApiResult<T> }>): Promise<T> {
  const response = await request
  return response.data.data
}
