<script lang="ts" setup>
import { computed } from 'vue';

import { AuthPageLayout } from '@vben/layouts';
import { preferences } from '@vben/preferences';

import { $t } from '#/locales';

const appName = computed(() => preferences.app.name);
const logo = computed(() => preferences.logo.source);
const logoDark = computed(() => preferences.logo.sourceDark);
const sloganImage = computed(() => '/brand/ones-1s-slogan.png');
const brandFeatures = computed(() => [
  {
    description: $t('authentication.visual.features.execution.description'),
    icon: 'lucide:cpu',
    placement: 'top' as const,
    subtitle: $t('authentication.visual.features.execution.subtitle'),
    title: $t('authentication.visual.features.execution.title'),
  },
  {
    description: $t('authentication.visual.features.sensing.description'),
    icon: 'lucide:eye',
    placement: 'rightTop' as const,
    subtitle: $t('authentication.visual.features.sensing.subtitle'),
    title: $t('authentication.visual.features.sensing.title'),
  },
  {
    description: $t('authentication.visual.features.security.description'),
    icon: 'lucide:shield-check',
    placement: 'rightMiddle' as const,
    subtitle: $t('authentication.visual.features.security.subtitle'),
    title: $t('authentication.visual.features.security.title'),
  },
  {
    description: $t('authentication.visual.features.service.description'),
    icon: 'lucide:layers-3',
    placement: 'rightBottom' as const,
    subtitle: $t('authentication.visual.features.service.subtitle'),
    title: $t('authentication.visual.features.service.title'),
  },
  {
    description: $t('authentication.visual.features.asset.description'),
    icon: 'lucide:database',
    placement: 'bottom' as const,
    subtitle: $t('authentication.visual.features.asset.subtitle'),
    title: $t('authentication.visual.features.asset.title'),
  },
  {
    description: $t('authentication.visual.features.evolution.description'),
    icon: 'lucide:chart-no-axes-column-increasing',
    placement: 'leftBottom' as const,
    subtitle: $t('authentication.visual.features.evolution.subtitle'),
    title: $t('authentication.visual.features.evolution.title'),
  },
  {
    description: $t('authentication.visual.features.connection.description'),
    icon: 'lucide:network',
    placement: 'leftMiddle' as const,
    subtitle: $t('authentication.visual.features.connection.subtitle'),
    title: $t('authentication.visual.features.connection.title'),
  },
  {
    description: $t('authentication.visual.features.intelligence.description'),
    icon: 'lucide:brain-circuit',
    placement: 'leftTop' as const,
    subtitle: $t('authentication.visual.features.intelligence.subtitle'),
    title: $t('authentication.visual.features.intelligence.title'),
  },
]);
const brandPrinciples = computed(() => [
  {
    description: $t('authentication.visual.principles.second.description'),
    icon: 'lucide:timer',
    title: $t('authentication.visual.principles.second.title'),
  },
  {
    description: $t('authentication.visual.principles.system.description'),
    icon: 'lucide:boxes',
    title: $t('authentication.visual.principles.system.title'),
  },
  {
    description: $t('authentication.visual.principles.service.description'),
    icon: 'lucide:badge-check',
    title: $t('authentication.visual.principles.service.title'),
  },
  {
    description: $t('authentication.visual.principles.goal.description'),
    icon: 'lucide:target',
    title: $t('authentication.visual.principles.goal.title'),
  },
]);
const clickLogo = () => {};
</script>

<template>
  <AuthPageLayout
    :app-name="appName"
    :brand-features="brandFeatures"
    :brand-principles="brandPrinciples"
    :dark-mode-label="$t('authentication.visual.mode.darkLabel')"
    :dark-mode-text="$t('authentication.visual.mode.darkText')"
    :light-mode-label="$t('authentication.visual.mode.lightLabel')"
    :light-mode-text="$t('authentication.visual.mode.lightText')"
    :logo="logo"
    :logo-dark="logoDark"
    :page-description="$t('authentication.pageDesc')"
    :page-title="$t('authentication.pageTitle')"
    :slogan-image="sloganImage"
    :click-logo="clickLogo"
  >
    <!-- 自定义工具栏 -->
    <!-- <template #toolbar></template> -->
  </AuthPageLayout>
</template>
