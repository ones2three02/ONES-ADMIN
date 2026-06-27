import { http, unwrap } from '@/api/http'
import type {
  SystemRole,
  SystemUser,
  UserCreatePayload,
  UserUpdatePayload
} from '@/types/system'

export function listUsersApi() {
  return unwrap<SystemUser[]>(http.get('/system/users'))
}

export function createUserApi(payload: UserCreatePayload) {
  return unwrap<SystemUser>(http.post('/system/users', payload))
}

export function updateUserApi(id: number, payload: UserUpdatePayload) {
  return unwrap<SystemUser>(http.put(`/system/users/${id}`, payload))
}

export function deleteUserApi(id: number) {
  return unwrap<void>(http.delete(`/system/users/${id}`))
}

export function listRolesApi() {
  return unwrap<SystemRole[]>(http.get('/system/roles'))
}
