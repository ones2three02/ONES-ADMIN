<script setup lang="ts">
import * as Icons from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'
import type { MenuItem } from '@/types/menu'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()

const activePath = computed(() => route.path)

function resolveIcon(name: string) {
  return Icons[name as keyof typeof Icons] || Icons.Grid
}

async function handleLogout() {
  await ElMessageBox.confirm('确认退出 ONES-ADMIN？', '退出登录', {
    confirmButtonText: '退出',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await authStore.logout()
  await router.replace('/login')
}

function flattenMenus(menus: MenuItem[]) {
  return menus.flatMap((menu) => (menu.children.length ? menu.children : [menu]))
}
</script>

<template>
  <el-container class="admin-shell">
    <el-aside class="admin-aside" width="248px">
      <div class="brand">
        <div class="brand-mark">O</div>
        <div>
          <strong>ONES-ADMIN</strong>
          <span>企业级管理平台</span>
        </div>
      </div>

      <el-menu :default-active="activePath" router class="admin-menu">
        <template v-for="menu in authStore.menus" :key="menu.path">
          <el-sub-menu v-if="menu.children.length" :index="menu.path">
            <template #title>
              <el-icon><component :is="resolveIcon(menu.icon)" /></el-icon>
              <span>{{ menu.title }}</span>
            </template>
            <el-menu-item v-for="child in menu.children" :key="child.path" :index="child.path">
              <el-icon><component :is="resolveIcon(child.icon)" /></el-icon>
              <span>{{ child.title }}</span>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="menu.path">
            <el-icon><component :is="resolveIcon(menu.icon)" /></el-icon>
            <span>{{ menu.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="admin-header">
        <div>
          <h1>{{ route.meta.title || '工作台' }}</h1>
          <p>Sa-Token 登录态、菜单权限与前端路由已连通</p>
        </div>
        <div class="header-actions">
          <el-tag type="success" effect="light">已登录</el-tag>
          <el-avatar :src="authStore.user?.avatar" :size="36">
            {{ authStore.user?.displayName?.slice(0, 1) }}
          </el-avatar>
          <span class="username">{{ authStore.user?.displayName }}</span>
          <el-button :icon="Icons.SwitchButton" circle @click="handleLogout" />
        </div>
      </el-header>

      <el-main class="admin-main">
        <RouterView :menus="flattenMenus(authStore.menus)" />
      </el-main>
    </el-container>
  </el-container>
</template>
