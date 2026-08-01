<script setup lang="ts">
import type { ToolbarType } from './types';

import { computed } from 'vue';

import { preferences } from '@vben/preferences';

import { VbenIcon } from '@vben-core/shadcn-ui';

import { LanguageToggle, ThemeToggle } from '../widgets';

interface Props {
  toolbarList?: ToolbarType[];
}

defineOptions({
  name: 'AuthenticationToolbar',
});

const props = withDefaults(defineProps<Props>(), {
  toolbarList: () => ['language', 'theme'],
});

const showLanguage = computed(() => props.toolbarList.includes('language'));
const showTheme = computed(() => props.toolbarList.includes('theme'));
const languageLabel = computed(() =>
  preferences.app.locale === 'zh-CN' ? '简体中文' : 'English',
);
</script>

<template>
  <div class="authentication-toolbar">
    <div
      v-if="showLanguage && preferences.widget.languageToggle"
      class="authentication-language"
    >
      <LanguageToggle />
      <span>{{ languageLabel }}</span>
      <VbenIcon icon="lucide:chevron-down" />
    </div>
    <div
      v-if="showTheme && preferences.widget.themeToggle"
      class="authentication-theme"
    >
      <ThemeToggle />
    </div>
  </div>
</template>

<style scoped>
.authentication-toolbar {
  position: absolute;
  top: 27px;
  right: 27px;
  z-index: 30;
  display: flex;
  align-items: center;
  gap: 10px;
}

.authentication-language,
.authentication-theme {
  position: relative;
  height: 40px;
  border: 1px solid rgba(224, 231, 240, 0.84);
  background: rgba(255, 255, 255, 0.68);
  box-shadow: 0 6px 18px rgba(92, 112, 144, 0.06);
  backdrop-filter: blur(14px);
}

.authentication-language {
  width: 144px;
  border-radius: 999px;
}

.authentication-language > span {
  position: absolute;
  top: 50%;
  left: 46px;
  color: #30394a;
  font-size: 14px;
  font-weight: 600;
  pointer-events: none;
  transform: translateY(-50%);
}

.authentication-language > svg {
  position: absolute;
  top: 50%;
  right: 13px;
  width: 15px;
  height: 15px;
  color: #919caf;
  pointer-events: none;
  transform: translateY(-50%);
}

.authentication-language :deep(button) {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  justify-content: flex-start;
  border-radius: 999px;
  padding: 0 15px;
  background: transparent;
  color: #2878ff;
}

.authentication-language :deep(button svg) {
  color: #2878ff;
}

.authentication-theme {
  width: 40px;
  border-radius: 50%;
}

.authentication-theme :deep(button) {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  padding: 9px !important;
  background: transparent;
}

.authentication-language :deep(button:focus-visible),
.authentication-theme :deep(button:focus-visible) {
  outline: 2px solid #2f7cff;
  outline-offset: 3px;
}

@media (max-width: 640px) {
  .authentication-toolbar {
    top: 20px;
    right: 16px;
  }

  .authentication-language {
    width: 40px;
  }

  .authentication-language > span,
  .authentication-language > svg {
    display: none;
  }
}
</style>
