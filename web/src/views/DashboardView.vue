<script setup lang="ts">
import { CircleCheck, Connection, Lock, Menu, Monitor, TrendCharts } from '@element-plus/icons-vue'
import { computed } from 'vue'

import { useAuthStore } from '@/stores/auth'
import type { MenuItem } from '@/types/menu'

const authStore = useAuthStore()

const menuCount = computed(() => countMenus(authStore.menus))
const quickMenus = computed(() => flattenMenus(authStore.menus).slice(0, 4))
const permissionText = computed(() => {
  if (!authStore.permissions.length) {
    return '暂无权限标识'
  }
  return authStore.permissions.slice(0, 2).join('、')
})

function countMenus(menus: MenuItem[]): number {
  return menus.reduce((total, menu) => total + 1 + countMenus(menu.children), 0)
}

function flattenMenus(menus: MenuItem[]) {
  return menus.flatMap((menu) => (menu.children.length ? menu.children : [menu]))
}
</script>

<template>
  <div class="page-stack">
    <section class="page-card workbench-header">
      <div class="workbench-profile">
        <el-avatar :src="authStore.user?.avatar" :size="48">
          {{ authStore.user?.displayName?.slice(0, 1) }}
        </el-avatar>
        <div>
          <h1>早安，{{ authStore.user?.displayName }}</h1>
          <p>{{ authStore.user?.username }}，欢迎进入 ONES-ADMIN 工作台。</p>
        </div>
      </div>
      <div class="workbench-actions">
        <el-button :icon="Monitor">系统监控</el-button>
        <el-button :icon="Connection" type="primary">权限配置</el-button>
      </div>
    </section>

    <section class="dashboard-grid">
      <article class="page-card metric-card">
        <div class="metric-title">
          <span>当前角色</span>
          <el-icon><Lock /></el-icon>
        </div>
        <strong class="metric-value">{{ authStore.roles.length }}</strong>
        <p>{{ authStore.roles.join('、') || '未分配角色' }}</p>
      </article>
      <article class="page-card metric-card">
        <div class="metric-title">
          <span>权限标识</span>
          <el-icon><CircleCheck /></el-icon>
        </div>
        <strong class="metric-value">{{ authStore.permissions.length }}</strong>
        <p>{{ permissionText }}</p>
      </article>
      <article class="page-card metric-card">
        <div class="metric-title">
          <span>菜单节点</span>
          <el-icon><Menu /></el-icon>
        </div>
        <strong class="metric-value">{{ menuCount }}</strong>
        <p>后端动态菜单已加载</p>
      </article>
      <article class="page-card metric-card">
        <div class="metric-title">
          <span>认证状态</span>
          <el-icon><TrendCharts /></el-icon>
        </div>
        <strong class="metric-value">在线</strong>
        <p>Authorization: Bearer</p>
      </article>
    </section>

    <section class="content-grid">
      <article class="page-card">
        <header class="card-header">
          <div>
            <h2>快捷导航</h2>
            <p class="card-subtitle">来自后端菜单权限，可随角色变更</p>
          </div>
          <el-button link type="primary">全部菜单</el-button>
        </header>
        <div class="page-card-body">
          <div class="quick-nav-grid">
            <RouterLink
              v-for="menu in quickMenus"
              :key="menu.path"
              class="quick-nav-item"
              :to="menu.path"
            >
              <el-icon :size="22"><Menu /></el-icon>
              <span>{{ menu.title }}</span>
            </RouterLink>
          </div>
        </div>
      </article>

      <article class="page-card">
        <header class="card-header">
          <div>
            <h2>待办事项</h2>
            <p class="card-subtitle">围绕企业后台基础能力推进</p>
          </div>
          <el-tag type="success" effect="light">进行中</el-tag>
        </header>
        <div class="page-card-body todo-list">
          <div class="todo-item">
            <div>
              <strong>接入 MyBatis-Plus 持久化</strong>
              <span>用户、角色、菜单从演示数据迁移到数据库</span>
            </div>
            <el-tag>后端</el-tag>
          </div>
          <div class="todo-item">
            <div>
              <strong>完善系统管理 CRUD</strong>
              <span>保持页面风格与 Vben 管理后台一致</span>
            </div>
            <el-tag type="warning">前端</el-tag>
          </div>
          <div class="todo-item">
            <div>
              <strong>预留第三方登录适配层</strong>
              <span>飞书扫码登录和 SSO 后续统一接入</span>
            </div>
            <el-tag type="info">认证</el-tag>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>
