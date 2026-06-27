<script setup lang="ts">
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<template>
  <section class="page-stack">
    <article class="page-card placeholder-header">
      <div class="placeholder-title">
        <div class="brand-mark">O</div>
        <div>
          <h1>{{ route.meta.title }}</h1>
          <p class="placeholder-copy">路由、菜单和权限入口已接入，下一阶段替换为真实数据表格。</p>
        </div>
      </div>
      <div class="workbench-actions">
        <el-button :icon="Refresh">刷新</el-button>
        <el-button :icon="Plus" type="primary">新增</el-button>
      </div>
    </article>

    <article class="page-card">
      <div class="table-toolbar">
        <el-input :prefix-icon="Search" clearable placeholder="请输入关键字" style="max-width: 280px" />
        <el-button>重置</el-button>
      </div>
      <div class="page-card-body">
        <el-empty description="该模块将在接入 MyBatis-Plus 后端接口后展示业务表格与表单" />
      </div>
    </article>
  </section>
</template>
