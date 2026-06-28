import { requestClient } from '#/api/request';

export namespace SystemDeptApi {
  export interface SystemDept {
    children?: SystemDept[];
    createTime?: string;
    id: string;
    name: string;
    pid?: string;
    remark?: string;
    status: 0 | 1;
  }
}

/**
 * 获取部门列表数据
 */
async function getDeptList() {
  return requestClient.get<Array<SystemDeptApi.SystemDept>>(
    '/system/dept/list',
  );
}

/**
 * 创建部门
 * @param data 部门数据
 */
async function createDept(
  data: Partial<Omit<SystemDeptApi.SystemDept, 'children' | 'id'>>,
) {
  return requestClient.post('/system/dept', data);
}

/**
 * 更新部门
 *
 * @param id 部门 ID
 * @param data 部门数据
 */
async function updateDept(
  id: string,
  data: Partial<Omit<SystemDeptApi.SystemDept, 'children' | 'id'>>,
) {
  return requestClient.put(`/system/dept/${id}`, data);
}

/**
 * 删除部门
 * @param id 部门 ID
 */
async function deleteDept(id: string) {
  return requestClient.delete(`/system/dept/${id}`);
}

export { createDept, deleteDept, getDeptList, updateDept };
