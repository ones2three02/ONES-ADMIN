<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<template>
  <section class="workbench">
    <header>
      <h2>{{ route.meta.title }}</h2>
      <p>该模块路由与菜单权限已接入，后续可继续扩展为真实 CRUD 页面。</p>
    </header>
    <el-empty description="业务表格与表单将在下一阶段接入 MyBatis-Plus 后端接口" />
  </section>
</template>
