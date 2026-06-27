<script setup lang="ts">
import { Lock, User } from '@element-plus/icons-vue'
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
    <section class="login-panel">
      <div class="login-copy">
        <div class="brand-row">
          <div class="brand-mark">O</div>
          <span>ONES-ADMIN</span>
        </div>
        <h1>企业级后台管理系统</h1>
        <p>Vue3 + Spring Boot + Sa-Token 的首版可运行闭环。</p>
      </div>

      <el-form class="login-form" :model="form" label-position="top" @submit.prevent="submitLogin">
        <h2>登录控制台</h2>
        <el-form-item label="用户名">
          <el-input v-model="form.username" :prefix-icon="User" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            :prefix-icon="Lock"
            autocomplete="current-password"
            show-password
            type="password"
          />
        </el-form-item>
        <el-button class="login-submit" type="primary" native-type="submit" :loading="loading">
          登录
        </el-button>
        <p class="login-hint">演示账号：admin / admin123</p>
      </el-form>
    </section>
  </main>
</template>
