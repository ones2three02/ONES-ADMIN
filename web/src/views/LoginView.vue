<script setup lang="ts">
import { Lock, Message, Monitor, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const form = reactive({
  username: 'admin',
  password: 'admin123'
})

async function submitLogin() {
  loading.value = true
  try {
    await authStore.login(form)
    ElMessage.success('登录成功')
    await router.replace('/dashboard')
  } catch (error) {
    const message = error instanceof Error ? error.message : '登录失败'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <aside class="login-aside">
      <div class="login-brand">
        <div class="brand-mark">O</div>
        <div>
          <p class="brand-title">ONES-ADMIN</p>
          <p class="brand-subtitle">Enterprise Console</p>
        </div>
      </div>

      <section class="login-copy">
        <h1>企业级后台管理系统</h1>
        <p>沿用 Vben Admin 的后台布局范式，统一认证、权限、菜单与系统管理工作台。</p>
        <div class="login-capability-list">
          <div class="login-capability">
            <strong>权限体系</strong>
            <span>Sa-Token 承载登录态、角色、权限标识和后续单点登录接入。</span>
          </div>
          <div class="login-capability">
            <strong>前端选型</strong>
            <span>Vue3、Vite、TypeScript、Element Plus，风格对齐 Vben web-ele。</span>
          </div>
          <div class="login-capability">
            <strong>动态菜单</strong>
            <span>菜单与按钮权限从后端加载，页面按权限渐进开放。</span>
          </div>
          <div class="login-capability">
            <strong>系统基建</strong>
            <span>用户、角色、菜单、审计与第三方登录按模块演进。</span>
          </div>
        </div>
      </section>

      <footer class="login-footer">ONES-ADMIN 基础架构选型验证版</footer>
    </aside>

    <section class="login-main">
      <div class="login-card">
        <h2>欢迎回来</h2>
        <p>请使用账号登录管理控制台</p>
        <el-segmented class="login-tabs" :model-value="'account'" :options="['account']">
          <template #default>
            <span>账号登录</span>
          </template>
        </el-segmented>

        <el-form class="login-form" :model="form" label-position="top" @submit.prevent="submitLogin">
          <el-form-item label="用户名">
            <el-input
              v-model="form.username"
              :prefix-icon="User"
              autocomplete="username"
              size="large"
            />
          </el-form-item>
          <el-form-item label="密码">
            <el-input
              v-model="form.password"
              :prefix-icon="Lock"
              autocomplete="current-password"
              show-password
              size="large"
              type="password"
            />
          </el-form-item>
          <div class="login-tools">
            <el-checkbox>记住我</el-checkbox>
            <el-button link type="primary">忘记密码</el-button>
          </div>
          <el-button
            class="login-submit"
            type="primary"
            native-type="submit"
            :loading="loading"
            size="large"
          >
            登录
          </el-button>
          <el-divider>其他登录方式</el-divider>
          <el-button :icon="Monitor" plain size="large">飞书扫码登录</el-button>
          <p class="login-hint">
            <el-icon><Message /></el-icon>
            <span>
              演示账号：admin / admin123。飞书扫码与 SSO 入口预留，后续接入统一认证适配层。
            </span>
          </p>
        </el-form>
      </div>
    </section>
  </main>
</template>
