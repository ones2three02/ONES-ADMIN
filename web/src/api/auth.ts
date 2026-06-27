import { http, unwrap } from '@/api/http'
import type { LoginRequest, LoginResponse, UserProfile } from '@/types/auth'
import type { MenuItem } from '@/types/menu'

export function loginApi(payload: LoginRequest) {
  return unwrap<LoginResponse>(http.post('/auth/login', payload))
}

export function getCurrentUserApi() {
  return unwrap<UserProfile>(http.get('/auth/me'))
}

export function logoutApi() {
  return unwrap<void>(http.post('/auth/logout'))
}

export function getMenusApi() {
  return unwrap<MenuItem[]>(http.get('/system/menus'))
}
