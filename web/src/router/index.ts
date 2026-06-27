import { createRouter, createWebHistory } from 'vue-router'

import AdminLayout from '@/layouts/AdminLayout.vue'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { guest: true }
    },
    {
      path: '/',
      component: AdminLayout,
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('@/views/DashboardView.vue')
        },
        {
          path: 'system/user',
          name: 'system-user',
          component: () => import('@/views/SystemPlaceholderView.vue'),
          meta: { title: '用户管理' }
        },
        {
          path: 'system/role',
          name: 'system-role',
          component: () => import('@/views/SystemPlaceholderView.vue'),
          meta: { title: '角色管理' }
        },
        {
          path: 'system/menu',
          name: 'system-menu',
          component: () => import('@/views/SystemPlaceholderView.vue'),
          meta: { title: '菜单管理' }
        }
      ]
    }
  ]
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()
  if (to.meta.guest) {
    return authStore.isLoggedIn ? '/dashboard' : true
  }
  if (!authStore.isLoggedIn) {
    return '/login'
  }
  await authStore.hydrateSession()
  return true
})

export default router
