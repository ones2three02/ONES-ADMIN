<script lang="ts" setup>
import { computed } from 'vue';

import { AuthPageLayout } from '@vben/layouts';
import { preferences } from '@vben/preferences';

const appName = computed(() => preferences.app.name);
const isChinese = computed(() => preferences.app.locale === 'zh-CN');
const pageTitle = computed(() =>
  isChinese.value
    ? '新一代企业协同管理平台'
    : 'A new generation of enterprise collaboration',
);
const pageDescription = computed(() =>
  isChinese.value
    ? '连接人、流程与数据，助力企业高效运营与持续增长'
    : 'Connect people, processes, and data for efficient, sustainable growth',
);
const logo = '/brand/ones-brand-mark.png';
const clickLogo = () => {};
</script>

<template>
  <AuthPageLayout
    :app-name="appName"
    :click-logo="clickLogo"
    :logo="logo"
    :logo-dark="logo"
    :page-description="pageDescription"
    :page-title="pageTitle"
    :toolbar-list="['language', 'theme']"
  />
</template>
