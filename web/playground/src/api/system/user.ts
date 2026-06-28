import type { Recordable } from '@vben/types';

import { requestClient } from '#/api/request';

interface BackendRole {
  code: string;
  enabled: boolean;
  id: number;
  name: string;
}

interface BackendUser {
  avatar: string;
  createdAt?: string;
  deptId?: number;
  displayName: string;
  enabled: boolean;
  id: number;
  lastLoginAt?: string;
  lockedUntil?: string;
  remark?: string;
  roles: string[];
  username: string;
}

export namespace SystemUserApi {
  export interface SystemUser {
    [key: string]: any;
    createTime?: string;
    deptId?: string;
    id: string;
    name: string;
    password?: string;
    permissions: string[];
    remark?: string;
    roleCodes?: string[];
    status: 0 | 1;
    username: string;
  }
}

function toSystemUser(user: BackendUser): SystemUserApi.SystemUser {
  return {
    avatar: user.avatar,
    createTime: user.createdAt,
    deptId: user.deptId ? String(user.deptId) : undefined,
    displayName: user.displayName,
    enabled: user.enabled,
    id: String(user.id),
    lastLoginAt: user.lastLoginAt,
    lockedUntil: user.lockedUntil,
    name: user.displayName,
    permissions: user.roles,
    remark: user.remark,
    roleCodes: user.roles,
    roleText: user.roles.join('、'),
    roles: user.roles,
    status: user.enabled ? 1 : 0,
    username: user.username,
  };
}

function filterUsers(
  users: SystemUserApi.SystemUser[],
  params: Recordable<any>,
) {
  return users.filter((user) => {
    const matchedName = params.name
      ? user.name.includes(params.name) || user.username.includes(params.name)
      : true;
    const matchedId = params.id ? user.id.includes(String(params.id)) : true;
    const matchedStatus =
      params.status === undefined || params.status === ''
        ? true
        : user.status === Number(params.status);
    const matchedDept = params.deptId
      ? String(user.deptId) === String(params.deptId)
      : true;
    return matchedName && matchedId && matchedStatus && matchedDept;
  });
}

/**
 * 获取用户列表数据
 */
async function getUserList(params: Recordable<any>) {
  const users = await requestClient.get<BackendUser[]>('/system/users');
  const filteredUsers = filterUsers(users.map(toSystemUser), params);
  return {
    items: filteredUsers,
    total: filteredUsers.length,
  };
}

/**
 * 创建用户
 * @param data 用户数据
 */
async function createUser(data: Omit<SystemUserApi.SystemUser, 'id'>) {
  return requestClient.post('/system/users', {
    deptId: data.deptId ? Number(data.deptId) : undefined,
    displayName: data.name,
    enabled: data.status === 1,
    password: data.password,
    remark: data.remark,
    roleCodes: data.roleCodes ?? [],
    username: data.username,
  });
}

/**
 * 更新用户
 *
 * @param id 用户 ID
 * @param data 用户数据
 */
async function updateUser(
  id: string,
  data: Omit<SystemUserApi.SystemUser, 'id'>,
) {
  const users = await requestClient.get<BackendUser[]>('/system/users');
  const current = users.find((user) => String(user.id) === String(id));
  if (!current) {
    throw new Error('用户不存在');
  }
  return requestClient.put(`/system/users/${id}`, {
    deptId: data.deptId ? Number(data.deptId) : current.deptId,
    displayName: data.name ?? current.displayName,
    enabled:
      data.status === undefined || data.status === null
        ? current.enabled
        : data.status === 1,
    remark: data.remark ?? current.remark,
    roleCodes: data.roleCodes ?? current.roles,
    ...(data.password ? { password: data.password } : {}),
  });
}

/**
 * 删除用户
 * @param id 用户 ID
 */
async function deleteUser(id: string) {
  return requestClient.delete(`/system/users/${id}`);
}

async function listRoleOptions() {
  const roles = await requestClient.get<BackendRole[]>('/system/roles');
  return roles
    .filter((role) => role.enabled)
    .map((role) => ({
      label: role.name,
      value: role.code,
    }));
}

export { createUser, deleteUser, getUserList, listRoleOptions, updateUser };
