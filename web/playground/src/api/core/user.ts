import type { UserInfo } from '@vben/types';

import { requestClient } from '#/api/request';

interface OnesUserProfile {
  avatar: string;
  displayName: string;
  id: number;
  username: string;
}

/**
 * 获取用户信息
 */
export async function getUserInfoApi() {
  const [profile, roles] = await Promise.all([
    requestClient.get<OnesUserProfile>('/auth/me'),
    requestClient.get<string[]>('/auth/roles'),
  ]);

  return {
    avatar: profile.avatar,
    desc: '',
    homePath: '/dashboard/overview',
    realName: profile.displayName,
    roles,
    token: '',
    userId: String(profile.id),
    username: profile.username,
  } satisfies UserInfo;
}
