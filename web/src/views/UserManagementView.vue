<script setup lang="ts">
import { Delete, Edit, Plus, Refresh, Search, UserFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { computed, reactive, ref } from 'vue'

import {
  createUserApi,
  deleteUserApi,
  listRolesApi,
  listUsersApi,
  updateUserApi
} from '@/api/system'
import type { SystemRole, SystemUser, UserCreatePayload, UserUpdatePayload } from '@/types/system'

interface UserForm {
  id: number | null
  username: string
  displayName: string
  password: string
  enabled: boolean
  roleCodes: string[]
}

const users = ref<SystemUser[]>([])
const roles = ref<SystemRole[]>([])
const keyword = ref('')
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const formRef = ref<FormInstance>()

const form = reactive<UserForm>({
  id: null,
  username: '',
  displayName: '',
  password: '',
  enabled: true,
  roleCodes: []
})

const rules = computed<FormRules<UserForm>>(() => ({
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  displayName: [{ required: true, message: '请输入显示名称', trigger: 'blur' }],
  password:
    dialogMode.value === 'create'
      ? [{ required: true, min: 6, message: '密码至少 6 位', trigger: 'blur' }]
      : []
}))

const filteredUsers = computed(() => {
  const searchText = keyword.value.trim().toLowerCase()
  if (!searchText) {
    return users.value
  }
  return users.value.filter((user) => {
    return [user.username, user.displayName, user.roles.join(',')]
      .join(' ')
      .toLowerCase()
      .includes(searchText)
  })
})

loadData()

async function loadData() {
  loading.value = true
  try {
    const [userList, roleList] = await Promise.all([listUsersApi(), listRolesApi()])
    users.value = userList
    roles.value = roleList
  } catch (error) {
    showError(error, '加载用户数据失败')
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(user: SystemUser) {
  dialogMode.value = 'edit'
  Object.assign(form, {
    id: user.id,
    username: user.username,
    displayName: user.displayName,
    password: '',
    enabled: user.enabled,
    roleCodes: [...user.roles]
  })
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function submitForm() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (dialogMode.value === 'create') {
      const payload: UserCreatePayload = {
        username: form.username,
        displayName: form.displayName,
        password: form.password,
        enabled: form.enabled,
        roleCodes: form.roleCodes
      }
      await createUserApi(payload)
      ElMessage.success('用户新增成功')
    } else if (form.id !== null) {
      const payload: UserUpdatePayload = {
        displayName: form.displayName,
        enabled: form.enabled,
        roleCodes: form.roleCodes
      }
      await updateUserApi(form.id, payload)
      ElMessage.success('用户更新成功')
    }
    dialogVisible.value = false
    await loadData()
  } catch (error) {
    showError(error, '保存用户失败')
  } finally {
    saving.value = false
  }
}

async function confirmDelete(user: SystemUser) {
  try {
    await ElMessageBox.confirm(`确认删除用户「${user.displayName}」？`, '删除用户', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await deleteUserApi(user.id)
    ElMessage.success('用户已删除')
    await loadData()
  } catch (error) {
    showError(error, '删除用户失败')
  }
}

function resetForm() {
  Object.assign(form, {
    id: null,
    username: '',
    displayName: '',
    password: '',
    enabled: true,
    roleCodes: []
  })
  formRef.value?.clearValidate()
}

function roleName(code: string) {
  return roles.value.find((role) => role.code === code)?.name || code
}

function showError(error: unknown, fallback: string) {
  const message = error instanceof Error ? error.message : fallback
  ElMessage.error(message)
}
</script>

<template>
  <section class="page-stack management-page">
    <article class="page-card placeholder-header">
      <div class="placeholder-title">
        <div class="brand-mark">
          <el-icon><UserFilled /></el-icon>
        </div>
        <div>
          <h1>用户管理</h1>
          <p class="placeholder-copy">维护后台账号、启停状态和角色分配。</p>
        </div>
      </div>
      <div class="workbench-actions">
        <el-button :icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
        <el-button :icon="Plus" type="primary" @click="openCreateDialog">新增用户</el-button>
      </div>
    </article>

    <article class="page-card">
      <div class="table-toolbar">
        <el-input
          v-model="keyword"
          :prefix-icon="Search"
          clearable
          placeholder="搜索用户名、名称或角色"
          style="max-width: 320px"
        />
        <span class="table-summary">共 {{ filteredUsers.length }} 条</span>
      </div>

      <el-table v-loading="loading" :data="filteredUsers" row-key="id">
        <el-table-column label="用户" min-width="220">
          <template #default="{ row }: { row: SystemUser }">
            <div class="user-cell">
              <el-avatar :src="row.avatar" :size="34">
                {{ row.displayName.slice(0, 1) }}
              </el-avatar>
              <div>
                <strong>{{ row.displayName }}</strong>
                <span>{{ row.username }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="角色" min-width="220">
          <template #default="{ row }: { row: SystemUser }">
            <div class="role-tags">
              <el-tag v-for="role in row.roles" :key="role" effect="light">
                {{ roleName(role) }}
              </el-tag>
              <el-tag v-if="!row.roles.length" type="info" effect="plain">未分配</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }: { row: SystemUser }">
            <el-tag :type="row.enabled ? 'success' : 'info'" effect="light">
              {{ row.enabled ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column align="right" label="操作" width="180">
          <template #default="{ row }: { row: SystemUser }">
            <el-button :icon="Edit" link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button :icon="Delete" link type="danger" @click="confirmDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </article>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增用户' : '编辑用户'"
      width="520px"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="dialogMode === 'edit'" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="显示名称" prop="displayName">
          <el-input v-model="form.displayName" placeholder="请输入显示名称" />
        </el-form-item>
        <el-form-item v-if="dialogMode === 'create'" label="初始密码" prop="password">
          <el-input v-model="form.password" placeholder="请输入初始密码" show-password type="password" />
        </el-form-item>
        <el-form-item label="角色" prop="roleCodes">
          <el-select v-model="form.roleCodes" multiple placeholder="请选择角色" style="width: 100%">
            <el-option
              v-for="role in roles"
              :key="role.code"
              :disabled="!role.enabled"
              :label="role.name + '（' + role.code + '）'"
              :value="role.code"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.enabled" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </section>
</template>
