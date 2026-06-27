export interface TokenInfo {
  tokenName: string
  tokenValue: string
  tokenPrefix: string
}

export interface UserProfile {
  id: number
  username: string
  displayName: string
  avatar: string
}

export interface LoginResponse {
  token: TokenInfo
  user: UserProfile
  roles: string[]
  permissions: string[]
}

export interface LoginRequest {
  username: string
  password: string
}
