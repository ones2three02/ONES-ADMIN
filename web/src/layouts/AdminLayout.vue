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
const currentTitle = computed(() => String(route.meta.title || '工作台'))
const menuTrail = computed(() => findMenuTrail(authStore.menus, route.path))
const tabMenus = computed(() => flattenMenus(authStore.menus).slice(0, 6))

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

function findMenuTrail(menus: MenuItem[], path: string, parents: MenuItem[] = []): MenuItem[] {
  for (const menu of menus) {
    const currentTrail = [...parents, menu]
    if (menu.path === path) {
      return currentTrail
    }
    const childTrail = findMenuTrail(menu.children, path, currentTrail)
    if (childTrail.length) {
      return childTrail
    }
  }
  return []
}
</script>

<template>
  <el-container class="admin-shell">
    <el-aside class="admin-aside" width="248px">
      <div class="admin-logo">
        <div class="brand-mark">O</div>
        <div>
          <p class="brand-title">ONES-ADMIN</p>
          <p class="brand-subtitle">Enterprise Console</p>
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

    <el-container class="admin-layout">
      <el-header class="admin-header">
        <div class="admin-toolbar">
          <div class="toolbar-left">
            <el-button :icon="Icons.Fold" text />
            <el-breadcrumb separator="/">
              <el-breadcrumb-item>首页</el-breadcrumb-item>
              <el-breadcrumb-item v-for="menu in menuTrail" :key="menu.path">
                {{ menu.title }}
              </el-breadcrumb-item>
              <el-breadcrumb-item v-if="!menuTrail.length">
                {{ currentTitle }}
              </el-breadcrumb-item>
            </el-breadcrumb>
          </div>

          <div class="toolbar-right">
            <el-button :icon="Icons.Search" text />
            <el-button :icon="Icons.Refresh" text />
            <el-button :icon="Icons.FullScreen" text />
            <el-button :icon="Icons.Bell" text />
            <el-dropdown trigger="click">
              <span class="user-entry">
                <el-avatar :src="authStore.user?.avatar" :size="28">
                  {{ authStore.user?.displayName?.slice(0, 1) }}
                </el-avatar>
                <span class="username">{{ authStore.user?.displayName }}</span>
                <el-icon><component :is="Icons.ArrowDown" /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item :icon="Icons.User">个人中心</el-dropdown-item>
                  <el-dropdown-item :icon="Icons.Setting">系统设置</el-dropdown-item>
                  <el-dropdown-item divided :icon="Icons.SwitchButton" @click="handleLogout">
                    退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>

        <div class="admin-tabs">
          <RouterLink
            v-for="menu in tabMenus"
            :key="menu.path"
            class="admin-tab"
            :class="{ 'is-active': activePath === menu.path }"
            :to="menu.path"
          >
            <el-icon><component :is="resolveIcon(menu.icon)" /></el-icon>
            <span>{{ menu.title }}</span>
          </RouterLink>
        </div>
      </el-header>

      <el-main class="admin-main">
        <RouterView :menus="flattenMenus(authStore.menus)" />
      </el-main>
    </el-container>
  </el-container>
</template>
