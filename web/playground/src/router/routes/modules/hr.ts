import type { RouteRecordRaw } from 'vue-router';

import { $t } from '#/locales';

const routes: RouteRecordRaw[] = [
  {
    meta: {
      icon: 'lucide:users',
      order: 9998,
      title: $t('hr.title'),
    },
    name: 'HRMS',
    path: '/hr',
    children: [
      {
        path: '/hr/overview',
        name: 'HrOverview',
        meta: {
          icon: 'lucide:chart-no-axes-combined',
          title: $t('hr.overview.title'),
        },
        component: () => import('#/views/hr/overview/index.vue'),
      },
      {
        path: '/hr/employee',
        name: 'HrEmployee',
        meta: {
          icon: 'lucide:users',
          title: $t('hr.employee.title'),
        },
        component: () => import('#/views/hr/employee/list.vue'),
      },
      {
        path: '/hr/position',
        name: 'HrPosition',
        meta: {
          icon: 'lucide:briefcase-business',
          title: $t('hr.position.title'),
        },
        component: () => import('#/views/hr/position/list.vue'),
      },
      {
        path: '/hr/job-grade',
        name: 'HrJobGrade',
        meta: {
          icon: 'lucide:layers-3',
          title: $t('hr.jobGrade.title'),
        },
        component: () => import('#/views/hr/job-grade/list.vue'),
      },
      {
        path: '/hr/contract',
        name: 'HrContract',
        meta: {
          icon: 'lucide:file-text',
          title: $t('hr.contract.title'),
        },
        component: () => import('#/views/hr/contract/list.vue'),
      },
      {
        path: '/hr/document-warning',
        name: 'HrDocumentWarning',
        meta: {
          icon: 'lucide:file-warning',
          title: $t('hr.documentWarning.title'),
        },
        component: () => import('#/views/hr/document-warning/list.vue'),
      },
      {
        path: '/hr/roster-import',
        name: 'HrRosterImport',
        meta: {
          icon: 'lucide:file-up',
          title: $t('hr.rosterImport.title'),
        },
        component: () => import('#/views/hr/roster-import/list.vue'),
      },
    ],
  },
];

export default routes;
