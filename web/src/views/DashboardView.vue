<script setup lang="ts">
import { computed } from 'vue'

import { useAuthStore } from '@/stores/auth'
import type { MenuItem } from '@/types/menu'

const authStore = useAuthStore()

const menuCount = computed(() => countMenus(authStore.menus))

function countMenus(menus: MenuItem[]): number {
  return menus.reduce((total, menu) => total + 1 + countMenus(menu.children), 0)
}
</script>

<template>
  <section class="dashboard-grid">
    <article class="metric-card">
      <span>当前用户</span>
      <strong>{{ authStore.user?.displayName }}</strong>
      <p>{{ authStore.user?.username }}</p>
    </article>
    <article class="metric-card">
      <span>角色</span>
      <strong>{{ authStore.roles.length }}</strong>
      <p>{{ authStore.roles.join('、') }}</p>
    </article>
    <article class="metric-card">
      <span>权限标识</span>
      <strong>{{ authStore.permissions.length }}</strong>
      <p>按钮与接口权限已由 Sa-Token 承载</p>
    </article>
    <article class="metric-card">
      <span>菜单节点</span>
      <strong>{{ menuCount }}</strong>
      <p>后端动态菜单已加载</p>
    </article>
  </section>

  <section class="workbench">
    <header>
      <h2>首版能力闭环</h2>
      <p>这不是静态原型，页面数据来自 Spring Boot 后端接口。</p>
    </header>
    <el-descriptions :column="2" border>
      <el-descriptions-item label="认证框架">Sa-Token</el-descriptions-item>
      <el-descriptions-item label="Token 位置">Authorization: Bearer</el-descriptions-item>
      <el-descriptions-item label="后端接口">/api/auth/login、/api/auth/me、/api/system/menus</el-descriptions-item>
      <el-descriptions-item label="后续扩展">第三方登录适配层、数据权限、多租户、MyBatis-Plus</el-descriptions-item>
    </el-descriptions>
  </section>
</template>
