import type { RouteRecordRaw } from 'vue-router';

import { $t } from '#/locales';

const routes: RouteRecordRaw[] = [
  {
    meta: {
      icon: 'lucide:layout-dashboard',
      order: -1,
      title: $t('page.dashboard.title'),
    },
    name: 'Dashboard',
    path: '/dashboard',
    redirect: '/dashboard/overview',
    children: [
      {
        name: 'SystemOverview',
        path: '/dashboard/overview',
        component: () => import('#/views/dashboard/overview/index.vue'),
        meta: {
          affixTab: true,
          icon: 'lucide:gauge',
          title: '系统概览',
          keepAlive: true,
        },
      },
    ],
  },
];

export default routes;
