<script setup lang="ts">
import type { ToolbarType } from './types';

import { computed } from 'vue';

import { preferences, usePreferences } from '@vben/preferences';

import { VbenIcon } from '@vben-core/shadcn-ui';

import { Copyright } from '../basic/copyright';
import AuthenticationFormView from './form.vue';
import OnesVisualStage from './ones-visual-stage.vue';
import Toolbar from './toolbar.vue';

interface Props {
  appName?: string;
  clickLogo?: () => void;
  copyright?: boolean;
  darkModeLabel?: string;
  darkModeText?: string;
  lightModeLabel?: string;
  lightModeText?: string;
  logo?: string;
  logoDark?: string;
  pageDescription?: string;
  pageTitle?: string;
  toolbar?: boolean;
  toolbarList?: ToolbarType[];
}

const props = withDefaults(defineProps<Props>(), {
  appName: '',
  clickLogo: () => {},
  copyright: true,
  darkModeLabel: 'Dark Mode',
  darkModeText: 'Night',
  lightModeLabel: 'Light Mode',
  lightModeText: 'Day',
  logo: '',
  logoDark: '',
  pageDescription: '',
  pageTitle: '',
  toolbar: true,
  toolbarList: () => ['language', 'theme'],
});

const { authPanelCenter, authPanelLeft, isDark } = usePreferences();

const formSide = computed(() => {
  if (authPanelCenter.value) return 'bottom';
  if (authPanelLeft.value) return 'left';
  return 'right';
});

const logoSrc = computed(() => {
  if (isDark.value && props.logoDark) return props.logoDark;
  return props.logo;
});

const capabilities = computed(() => {
  const isChinese = preferences.app.locale === 'zh-CN';
  return [
    {
      description: isChinese ? '提升团队效率' : 'Improve team efficiency',
      icon: 'lucide:sparkles',
      title: isChinese ? '高效协同' : 'Efficient collaboration',
    },
    {
      description: isChinese ? '优化业务流程' : 'Optimize business processes',
      icon: 'lucide:workflow',
      title: isChinese ? '流程驱动' : 'Workflow driven',
    },
    {
      description: isChinese ? '辅助决策分析' : 'Support informed decisions',
      icon: 'lucide:chart-no-axes-column-increasing',
      title: isChinese ? '数据洞察' : 'Data insights',
    },
    {
      description: isChinese ? '企业级安全防护' : 'Enterprise-grade protection',
      icon: 'lucide:shield-check',
      title: isChinese ? '安全可靠' : 'Secure and reliable',
    },
  ];
});

const metrics = computed(() => {
  const isChinese = preferences.app.locale === 'zh-CN';
  return [
    { label: isChinese ? '业务模块' : 'Business modules', value: '20+' },
    { label: isChinese ? '企业服务' : 'Enterprise clients', value: '100+' },
    { label: isChinese ? '用户信任' : 'Trusted users', value: '10W+' },
    { label: isChinese ? '系统可用性' : 'System availability', value: '99.9%' },
  ];
});
</script>

<template>
  <div
    data-testid="login-shell"
    :class="{ dark: isDark }"
    class="authentication-shell"
  >
    <main class="authentication-canvas" :data-form-side="formSide">
      <template v-if="toolbar">
        <slot name="toolbar">
          <Toolbar :toolbar-list="toolbarList" />
        </slot>
      </template>

      <slot name="logo">
        <button
          v-if="logoSrc || appName"
          class="authentication-brand"
          type="button"
          @click="clickLogo"
        >
          <span class="authentication-brand-mark">
            <img v-if="logoSrc" :alt="appName" :src="logoSrc" />
          </span>
          <strong v-if="appName">{{ appName }}</strong>
        </button>
      </slot>

      <section v-if="!authPanelCenter" class="authentication-hero">
        <header class="authentication-hero-copy">
          <p class="authentication-eyebrow">ONES / 1S</p>
          <h1>{{ pageTitle }}</h1>
          <p class="authentication-description">{{ pageDescription }}</p>
        </header>

        <ul class="authentication-capabilities">
          <li v-for="capability in capabilities" :key="capability.title">
            <VbenIcon :icon="capability.icon" />
            <span>
              <strong>{{ capability.title }}</strong>
              <small>{{ capability.description }}</small>
            </span>
          </li>
        </ul>

        <OnesVisualStage class="authentication-visual" />

        <dl class="authentication-metrics">
          <div v-for="metric in metrics" :key="metric.value">
            <dt>{{ metric.value }}</dt>
            <dd>{{ metric.label }}</dd>
          </div>
        </dl>
      </section>

      <AuthenticationFormView
        class="authentication-form-panel"
        :data-side="formSide"
      />

      <footer v-if="copyright" class="authentication-copyright">
        <slot name="copyright">
          <Copyright
            v-if="preferences.copyright.enable"
            v-bind="preferences.copyright"
          />
        </slot>
      </footer>
    </main>
  </div>
</template>

<style scoped>
.authentication-shell {
  box-sizing: border-box;
  width: 100%;
  height: 100dvh;
  min-height: 100dvh;
  overflow: hidden;
  padding: 8px;
  background: #edf2f7;
  color: #182236;
}

.authentication-canvas {
  position: relative;
  display: grid;
  width: 100%;
  height: 100%;
  grid-template-columns: minmax(0, 58.3fr) minmax(0, 41.7fr);
  overflow: hidden;
  border: 1px solid rgba(205, 217, 232, 0.72);
  border-radius: 28px;
  background: #f8fbff;
  box-shadow: 0 18px 54px rgba(72, 92, 122, 0.12);
}

.authentication-canvas[data-form-side='left'] {
  grid-template-columns: minmax(0, 41.7fr) minmax(0, 58.3fr);
}

.authentication-canvas[data-form-side='bottom'] {
  display: block;
}

.authentication-brand {
  position: absolute;
  top: 27px;
  left: 31px;
  z-index: 20;
  display: inline-flex;
  height: 40px;
  align-items: center;
  gap: 12px;
  border: 0;
  padding: 0;
  background: transparent;
  color: #1d2638;
  cursor: pointer;
}

.authentication-brand:focus-visible {
  border-radius: 9px;
  outline: 2px solid #2f7cff;
  outline-offset: 4px;
}

.authentication-brand-mark {
  display: inline-flex;
  width: 40px;
  height: 40px;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 9px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 6px 18px rgba(68, 108, 177, 0.1);
}

.authentication-brand-mark img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.authentication-brand strong {
  font-size: 21px;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.authentication-hero {
  position: relative;
  min-width: 0;
  overflow: hidden;
  background:
    radial-gradient(circle at 52% 48%, rgba(222, 236, 255, 0.82), transparent 42%),
    linear-gradient(145deg, #fbfdff 0%, #f4f8fe 54%, #edf5ff 100%);
}

.authentication-canvas[data-form-side='left'] .authentication-hero {
  grid-column: 2;
}

.authentication-canvas[data-form-side='left'] .authentication-form-panel {
  grid-row: 1;
  grid-column: 1;
}

.authentication-hero-copy {
  position: absolute;
  top: 112px;
  right: 80px;
  left: 79px;
  z-index: 3;
}

.authentication-eyebrow {
  margin: 0 0 12px;
  color: #2878ff;
  font-size: 16px;
  font-weight: 800;
}

.authentication-hero h1 {
  margin: 0;
  color: #172134;
  font-size: clamp(32px, 2.54vw, 39px);
  font-weight: 800;
  letter-spacing: 0.015em;
  line-height: 1.2;
}

.authentication-description {
  margin: 12px 0 0;
  color: #556174;
  font-size: 16px;
  font-weight: 500;
  line-height: 1.6;
}

.authentication-capabilities {
  position: absolute;
  top: 267px;
  right: 76px;
  left: 79px;
  z-index: 3;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin: 0;
  padding: 0;
  list-style: none;
}

.authentication-capabilities li {
  display: flex;
  min-width: 0;
  align-items: flex-start;
  gap: 10px;
  color: #2878ff;
}

.authentication-capabilities svg {
  width: 18px;
  height: 18px;
  flex: 0 0 18px;
  margin-top: 1px;
}

.authentication-capabilities span {
  display: flex;
  min-width: 0;
  flex-direction: column;
}

.authentication-capabilities strong {
  color: #2b3445;
  font-size: 15px;
  font-weight: 700;
  line-height: 1.3;
  overflow-wrap: anywhere;
}

.authentication-capabilities small {
  margin-top: 4px;
  color: #8290a5;
  font-size: 12px;
  font-weight: 500;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.authentication-visual {
  top: 322px !important;
  right: 4px !important;
  bottom: auto !important;
  left: -8px !important;
  height: 500px !important;
  min-height: 0 !important;
}

.authentication-metrics {
  position: absolute;
  right: 79px;
  bottom: 76px;
  left: 79px;
  z-index: 4;
  display: grid;
  height: 100px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin: 0;
  border: 1px solid rgba(221, 231, 244, 0.68);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: 0 12px 32px rgba(98, 128, 170, 0.08);
  backdrop-filter: blur(18px);
}

.authentication-metrics > div {
  position: relative;
  display: flex;
  min-width: 0;
  flex-direction: column;
  justify-content: center;
  padding-left: 34px;
}

.authentication-metrics > div:not(:last-child)::after {
  position: absolute;
  top: 23px;
  right: 0;
  width: 1px;
  height: 54px;
  background: #ebeff5;
  content: '';
}

.authentication-metrics dt {
  color: #172134;
  font-size: 25px;
  font-weight: 800;
  line-height: 1.1;
}

.authentication-metrics dd {
  margin: 8px 0 0;
  color: #728096;
  font-size: 14px;
  font-weight: 500;
}

.authentication-form-panel {
  min-width: 0;
  background:
    radial-gradient(circle at 72% 34%, rgba(235, 243, 255, 0.72), transparent 38%),
    #f9fbfe;
}

.authentication-copyright {
  position: absolute;
  right: 18%;
  bottom: 17px;
  left: 18%;
  z-index: 10;
  display: flex;
  justify-content: center;
  color: #6f7c91;
  font-size: 13px;
  white-space: nowrap;
}

.dark.authentication-shell {
  background: #e8edf4;
}

@media (max-height: 900px) and (min-width: 1280px) {
  .authentication-hero-copy {
    top: clamp(82px, 11.2vh, 101px);
  }

  .authentication-eyebrow {
    margin-bottom: 7px;
  }

  .authentication-description {
    margin-top: 7px;
  }

  .authentication-capabilities {
    top: clamp(205px, 27.2vh, 245px);
  }

  .authentication-visual {
    top: clamp(238px, 31vh, 279px) !important;
    height: clamp(330px, 45vh, 405px) !important;
  }

  .authentication-metrics {
    bottom: 54px;
    height: 82px;
  }

  .authentication-metrics > div:not(:last-child)::after {
    top: 18px;
    height: 46px;
  }

  .authentication-copyright {
    bottom: 12px;
  }
}

@media (max-width: 1279px) {
  .authentication-canvas,
  .authentication-canvas[data-form-side='left'] {
    display: block;
  }

  .authentication-hero {
    display: none;
  }

  .authentication-form-panel {
    width: 100%;
    height: 100%;
  }
}

@media (max-width: 640px) {
  .authentication-shell {
    padding: 0;
  }

  .authentication-canvas {
    border: 0;
    border-radius: 0;
  }

  .authentication-brand {
    top: 20px;
    left: 20px;
  }

  .authentication-brand strong {
    font-size: 18px;
  }

  .authentication-copyright {
    right: 16px;
    bottom: 12px;
    left: 16px;
    overflow: hidden;
    font-size: 11px;
    text-overflow: ellipsis;
  }
}
</style>
