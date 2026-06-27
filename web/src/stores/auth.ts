import { defineStore } from 'pinia'

import { getCurrentUserApi, getMenusApi, loginApi, logoutApi } from '@/api/auth'
import type { LoginRequest, UserProfile } from '@/types/auth'
import type { MenuItem } from '@/types/menu'

const TOKEN_VALUE_KEY = 'ones-admin-token-value'
const TOKEN_PREFIX_KEY = 'ones-admin-token-prefix'

interface AuthState {
  tokenValue: string
  tokenPrefix: string
  user: UserProfile | null
  roles: string[]
  permissions: string[]
  menus: MenuItem[]
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    tokenValue: localStorage.getItem(TOKEN_VALUE_KEY) || '',
    tokenPrefix: localStorage.getItem(TOKEN_PREFIX_KEY) || 'Bearer',
    user: null,
    roles: [],
    permissions: [],
    menus: []
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.tokenValue)
  },
  actions: {
    async login(payload: LoginRequest) {
      const response = await loginApi(payload)
      this.tokenValue = response.token.tokenValue
      this.tokenPrefix = response.token.tokenPrefix
      this.user = response.user
      this.roles = response.roles
      this.permissions = response.permissions
      localStorage.setItem(TOKEN_VALUE_KEY, this.tokenValue)
      localStorage.setItem(TOKEN_PREFIX_KEY, this.tokenPrefix)
      await this.loadMenus()
    },
    async hydrateSession() {
      if (!this.tokenValue || this.user) {
        return
      }
      this.user = await getCurrentUserApi()
      await this.loadMenus()
    },
    async loadMenus() {
      this.menus = await getMenusApi()
    },
    async logout() {
      if (this.tokenValue) {
        await logoutApi().catch(() => undefined)
      }
      this.clearSession()
    },
    clearSession() {
      this.tokenValue = ''
      this.tokenPrefix = 'Bearer'
      this.user = null
      this.roles = []
      this.permissions = []
      this.menus = []
      localStorage.removeItem(TOKEN_VALUE_KEY)
      localStorage.removeItem(TOKEN_PREFIX_KEY)
    }
  }
})
