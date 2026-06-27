export interface SystemUser {
  id: number
  username: string
  displayName: string
  avatar: string
  enabled: boolean
  roles: string[]
}

export interface SystemRole {
  id: number
  code: string
  name: string
  enabled: boolean
}

export interface UserCreatePayload {
  username: string
  displayName: string
  password: string
  enabled: boolean
  roleCodes: string[]
}

export interface UserUpdatePayload {
  displayName: string
  enabled: boolean
  roleCodes: string[]
}
