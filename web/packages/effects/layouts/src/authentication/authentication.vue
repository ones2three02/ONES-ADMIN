<script setup lang="ts">
import type { ToolbarType } from './types';

import { computed } from 'vue';

import { preferences, usePreferences } from '@vben/preferences';

import { VbenIcon } from '@vben-core/shadcn-ui';

import { Copyright } from '../basic/copyright';
import AuthenticationFormView from './form.vue';
import SloganIcon from './icons/slogan.vue';
import Toolbar from './toolbar.vue';

type BrandFeaturePlacement =
  | 'bottom'
  | 'leftBottom'
  | 'leftMiddle'
  | 'leftTop'
  | 'rightBottom'
  | 'rightMiddle'
  | 'rightTop'
  | 'top';

interface BrandFeature {
  description?: string;
  icon?: string;
  placement?: BrandFeaturePlacement;
  subtitle?: string;
  title: string;
}

interface BrandPrinciple {
  description?: string;
  icon?: string;
  title: string;
}

interface Props {
  appName?: string;
  brandFeatures?: BrandFeature[];
  brandPrinciples?: BrandPrinciple[];
  darkModeLabel?: string;
  darkModeText?: string;
  lightModeLabel?: string;
  lightModeText?: string;
  logo?: string;
  logoDark?: string;
  pageTitle?: string;
  pageDescription?: string;
  sloganImage?: string;
  toolbar?: boolean;
  copyright?: boolean;
  toolbarList?: ToolbarType[];
  clickLogo?: () => void;
}

const props = withDefaults(defineProps<Props>(), {
  appName: '',
  brandFeatures: () => [],
  brandPrinciples: () => [],
  copyright: true,
  darkModeLabel: 'Dark Mode',
  darkModeText: 'Night',
  lightModeLabel: 'Light Mode',
  lightModeText: 'Day',
  logo: '',
  logoDark: '',
  pageDescription: '',
  pageTitle: '',
  sloganImage: '',
  toolbar: true,
  toolbarList: () => ['color', 'language', 'layout', 'theme'],
  clickLogo: () => {},
});

const { authPanelCenter, authPanelLeft, authPanelRight, isDark } =
  usePreferences();

const featurePlacementMap: Record<BrandFeaturePlacement, string> = {
  bottom: 'login-node-bottom',
  leftBottom: 'login-node-left-bottom',
  leftMiddle: 'login-node-left-middle',
  leftTop: 'login-node-left-top',
  rightBottom: 'login-node-right-bottom',
  rightMiddle: 'login-node-right-middle',
  rightTop: 'login-node-right-top',
  top: 'login-node-top',
};

const visualFeatures = computed(() => props.brandFeatures);
const visualPrinciples = computed(() => props.brandPrinciples);
const modeLabel = computed(() =>
  isDark.value ? props.darkModeLabel : props.lightModeLabel,
);
const modeText = computed(() =>
  isDark.value ? props.darkModeText : props.lightModeText,
);

function getFeaturePlacementClass(placement: BrandFeaturePlacement = 'top') {
  return featurePlacementMap[placement];
}

/**
 * @zh_CN 根据主题选择合适的 logo 图标
 */
const logoSrc = computed(() => {
  // 如果是暗色主题且提供了 logoDark，则使用暗色主题的 logo
  if (isDark.value && props.logoDark) {
    return props.logoDark;
  }
  // 否则使用默认的 logo
  return props.logo;
});
</script>

<template>
  <div
    :class="[isDark ? 'dark' : '']"
    class="flex min-h-full flex-1 overflow-x-hidden select-none"
  >
    <template v-if="toolbar">
      <slot name="toolbar">
        <Toolbar :toolbar-list="toolbarList" />
      </slot>
    </template>
    <!-- 左侧认证面板 -->
    <AuthenticationFormView
      v-if="authPanelLeft"
      class="min-h-full w-full xl:w-2/5"
      data-side="left"
    >
      <template v-if="copyright" #copyright>
        <slot name="copyright">
          <Copyright
            v-if="preferences.copyright.enable"
            v-bind="preferences.copyright"
          />
        </slot>
      </template>
    </AuthenticationFormView>

    <slot name="logo">
      <!-- 头部 Logo 和应用名称 -->
      <div
        v-if="logoSrc || appName"
        class="absolute top-0 left-0 z-10 flex flex-1"
        @click="clickLogo"
      >
        <div
          class="mt-4 ml-4 flex flex-1 items-center text-foreground sm:top-6 sm:left-6 lg:text-foreground"
        >
          <img
            v-if="logoSrc"
            :key="logoSrc"
            :alt="appName"
            :src="logoSrc"
            class="mr-2"
            width="42"
          />
          <p v-if="appName" class="m-0 text-xl font-medium">
            {{ appName }}
          </p>
        </div>
      </div>
    </slot>

    <!-- 系统介绍 -->
    <div v-if="!authPanelCenter" class="relative hidden w-0 flex-1 xl:block">
      <div
        class="absolute inset-0 size-full bg-background-deep dark:bg-[#070709] login-left-bg"
      >
        <div class="login-background absolute top-0 left-0 size-full"></div>
        <div class="login-grid absolute inset-0"></div>
        <div
          :key="authPanelLeft ? 'left' : authPanelRight ? 'right' : 'center'"
          class="login-visual-content flex-col-center h-full"
          :class="{
            'enter-x': authPanelLeft,
            '-enter-x': authPanelRight,
          }"
        >
          <div class="login-visual-inner">
            <div class="login-visual-header">
              <div class="login-mode-pill">
                <VbenIcon
                  :icon="isDark ? 'lucide:moon-star' : 'lucide:sun'"
                  class="size-4"
                />
                <span>{{ modeText }}</span>
              </div>
              <span class="login-mode-label">{{ modeLabel }}</span>
            </div>

            <div class="login-visual-stage">
              <div class="login-orbit login-orbit-outer"></div>
              <div class="login-orbit login-orbit-middle"></div>
              <div class="login-orbit login-orbit-inner"></div>
              <div class="login-scan-line"></div>

              <div
                v-for="feature in visualFeatures"
                :key="feature.title"
                class="login-capability-node"
                :class="getFeaturePlacementClass(feature.placement)"
              >
                <span class="login-node-icon">
                  <VbenIcon
                    v-if="feature.icon"
                    :icon="feature.icon"
                    class="size-5"
                  />
                </span>
                <span class="login-node-copy">
                  <strong>{{ feature.title }}</strong>
                  <small v-if="feature.subtitle">{{ feature.subtitle }}</small>
                  <em v-if="feature.description">{{ feature.description }}</em>
                </span>
              </div>

              <div class="login-core-visual">
                <template v-if="sloganImage">
                  <img :alt="appName" :src="sloganImage" />
                </template>
                <SloganIcon v-else :alt="appName" />
              </div>
            </div>

            <div class="login-visual-title">
              <div>{{ pageTitle }}</div>
              <p>{{ pageDescription }}</p>
            </div>

            <div class="login-principles" v-if="visualPrinciples.length > 0">
              <div
                v-for="principle in visualPrinciples"
                :key="principle.title"
                class="login-principle-item"
              >
                <span>
                  <VbenIcon
                    v-if="principle.icon"
                    :icon="principle.icon"
                    class="size-4"
                  />
                </span>
                <div>
                  <strong>{{ principle.title }}</strong>
                  <small v-if="principle.description">
                    {{ principle.description }}
                  </small>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 中心认证面板 -->
    <div v-if="authPanelCenter" class="relative flex-center w-full">
      <div class="login-background absolute top-0 left-0 size-full"></div>
      <AuthenticationFormView
        class="w-full rounded-3xl pb-20 shadow-float shadow-primary/5 md:w-2/3 md:bg-background lg:w-1/2 xl:w-[36%]"
        data-side="bottom"
      >
        <template v-if="copyright" #copyright>
          <slot name="copyright">
            <Copyright
              v-if="preferences.copyright.enable"
              v-bind="preferences.copyright"
            />
          </slot>
        </template>
      </AuthenticationFormView>
    </div>

    <!-- 右侧认证面板 -->
    <AuthenticationFormView
      v-if="authPanelRight"
      class="min-h-full w-full xl:w-2/5"
      data-side="right"
    >
      <template v-if="copyright" #copyright>
        <slot name="copyright">
          <Copyright
            v-if="preferences.copyright.enable"
            v-bind="preferences.copyright"
          />
        </slot>
      </template>
    </AuthenticationFormView>
  </div>
</template>

<style scoped>
.login-left-bg {
  background:
    radial-gradient(circle at 46% 40%, rgba(37, 99, 235, 14%), transparent 34%),
    radial-gradient(circle at 76% 24%, rgba(99, 102, 241, 16%), transparent 28%),
    radial-gradient(circle at 24% 76%, rgba(6, 182, 212, 10%), transparent 30%),
    linear-gradient(135deg, #fbfdff 0%, #f3f7ff 48%, #eef5ff 100%);
  border-right: 1px solid rgba(37, 99, 235, 10%);
  color: #0b1536;
}

.login-background {
  background:
    radial-gradient(circle at 50% 50%, rgba(37, 99, 235, 22%) 0%, transparent 58%),
    radial-gradient(circle at 30% 35%, rgba(124, 58, 237, 16%) 0%, transparent 44%),
    radial-gradient(circle at 70% 58%, rgba(6, 182, 212, 14%) 0%, transparent 46%);
  filter: blur(74px);
}

.login-grid {
  background-image:
    linear-gradient(rgba(37, 99, 235, 6%) 1px, transparent 1px),
    linear-gradient(90deg, rgba(37, 99, 235, 6%) 1px, transparent 1px);
  background-size: 32px 32px;
  opacity: 0.78;
  mask-image: linear-gradient(to bottom, transparent, black 12%, black 82%, transparent);
}

.login-visual-content {
  padding: 48px 72px 40px 56px;
}

.login-visual-inner {
  position: relative;
  z-index: 1;
  display: flex;
  box-sizing: border-box;
  width: min(100%, 760px);
  height: min(82vh, 720px);
  min-height: 620px;
  flex-direction: column;
  justify-content: space-between;
  overflow: hidden;
  border: 1px solid rgba(37, 99, 235, 12%);
  border-radius: 28px;
  padding: 22px 28px 24px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 78%), rgba(255, 255, 255, 54%)),
    radial-gradient(circle at 50% 42%, rgba(37, 99, 235, 12%), transparent 48%);
  box-shadow:
    0 28px 80px rgba(37, 99, 235, 14%),
    inset 0 1px 0 rgba(255, 255, 255, 72%);
  backdrop-filter: blur(18px);
}

.login-visual-inner::before {
  position: absolute;
  inset: 18px;
  border: 1px solid rgba(37, 99, 235, 7%);
  border-radius: 22px;
  pointer-events: none;
  content: '';
}

.login-visual-header {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 10px;
  color: rgba(11, 21, 54, 64%);
  font-size: 12px;
  font-weight: 600;
}

.login-mode-pill {
  display: inline-flex;
  height: 32px;
  align-items: center;
  gap: 8px;
  border: 1px solid rgba(37, 99, 235, 14%);
  border-radius: 999px;
  padding: 0 12px;
  background: rgba(255, 255, 255, 76%);
  color: #2563eb;
  box-shadow: 0 10px 28px rgba(37, 99, 235, 12%);
}

.login-mode-label {
  text-transform: uppercase;
}

.login-visual-stage {
  position: relative;
  isolation: isolate;
  height: 490px;
  margin-top: 6px;
}

.login-visual-stage::before {
  position: absolute;
  top: 56%;
  left: 50%;
  z-index: -1;
  width: 430px;
  height: 168px;
  border-radius: 50%;
  background:
    radial-gradient(circle, rgba(37, 99, 235, 22%), transparent 62%),
    radial-gradient(circle, rgba(6, 182, 212, 18%), transparent 70%);
  filter: blur(8px);
  transform: translate(-50%, -50%) rotate(-6deg);
  content: '';
}

.login-visual-stage::after {
  position: absolute;
  top: 50%;
  left: 50%;
  z-index: -1;
  width: 600px;
  height: 360px;
  border-radius: 50%;
  background:
    linear-gradient(90deg, transparent 49%, rgba(37, 99, 235, 18%) 50%, transparent 51%),
    linear-gradient(0deg, transparent 49%, rgba(37, 99, 235, 12%) 50%, transparent 51%);
  opacity: 0.7;
  transform: translate(-50%, -50%) rotate(-10deg);
  content: '';
}

.login-orbit {
  position: absolute;
  inset: 50%;
  border: 1px solid rgba(37, 99, 235, 22%);
  border-style: dashed;
  border-radius: 50%;
  box-shadow: 0 0 32px rgba(37, 99, 235, 10%);
  transform: translate(-50%, -50%);
}

.login-orbit::before,
.login-orbit::after {
  position: absolute;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #2563eb;
  box-shadow: 0 0 16px rgba(37, 99, 235, 62%);
  content: '';
}

.login-orbit::before {
  top: 18%;
  right: 13%;
}

.login-orbit::after {
  bottom: 14%;
  left: 21%;
}

.login-orbit-outer {
  width: 540px;
  height: 320px;
  transform: translate(-50%, -50%) rotate(-10deg);
}

.login-orbit-middle {
  width: 450px;
  height: 270px;
  opacity: 0.76;
  transform: translate(-50%, -50%) rotate(12deg);
}

.login-orbit-inner {
  width: 330px;
  height: 205px;
  opacity: 0.62;
  transform: translate(-50%, -50%) rotate(-24deg);
}

.login-scan-line {
  position: absolute;
  top: 11%;
  left: 50%;
  width: 1px;
  height: 78%;
  background: linear-gradient(transparent, rgba(37, 99, 235, 46%), transparent);
  box-shadow: 0 0 18px rgba(37, 99, 235, 28%);
  transform: translateX(-50%);
}

.login-core-visual {
  position: absolute;
  top: 50%;
  left: 50%;
  z-index: 1;
  width: 336px;
  height: 336px;
  overflow: hidden;
  border: 1px solid rgba(37, 99, 235, 18%);
  border-radius: 34px;
  background: rgba(255, 255, 255, 36%);
  box-shadow:
    0 30px 86px rgba(37, 99, 235, 22%),
    0 0 0 12px rgba(255, 255, 255, 34%),
    inset 0 1px 0 rgba(255, 255, 255, 58%);
  animation: login-core-float 5s ease-in-out infinite;
  transform: translate(-50%, -50%);
}

.login-core-visual::after {
  position: absolute;
  inset: 14px;
  border: 1px solid rgba(125, 211, 252, 24%);
  border-radius: 26px;
  box-shadow: inset 0 0 32px rgba(37, 99, 235, 20%);
  pointer-events: none;
  content: '';
}

.login-core-visual img,
.login-core-visual svg {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.login-capability-node {
  position: absolute;
  z-index: 2;
  display: flex;
  width: 178px;
  min-height: 70px;
  align-items: flex-start;
  gap: 10px;
  border: 1px solid rgba(37, 99, 235, 10%);
  border-radius: 18px;
  padding: 10px;
  background: rgba(255, 255, 255, 66%);
  color: rgba(11, 21, 54, 74%);
  box-shadow: 0 16px 44px rgba(37, 99, 235, 10%);
  backdrop-filter: blur(16px);
}

.login-node-icon {
  display: inline-flex;
  width: 42px;
  height: 42px;
  flex: 0 0 42px;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(37, 99, 235, 15%);
  border-radius: 999px;
  background: rgba(255, 255, 255, 82%);
  color: #2563eb;
  box-shadow:
    0 14px 34px rgba(37, 99, 235, 16%),
    inset 0 0 18px rgba(37, 99, 235, 8%);
}

.login-node-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
}

.login-node-copy strong {
  color: #0b1536;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.2;
}

.login-node-copy small {
  margin-top: 2px;
  color: rgba(11, 21, 54, 58%);
  font-size: 11px;
  font-weight: 600;
  line-height: 1.2;
}

.login-node-copy em {
  margin-top: 5px;
  color: rgba(11, 21, 54, 48%);
  font-size: 11px;
  font-style: normal;
  line-height: 1.35;
}

.login-node-top {
  top: 0;
  left: 50%;
  transform: translateX(-50%);
}

.login-node-right-top {
  top: 90px;
  right: 0;
}

.login-node-right-middle {
  top: 222px;
  right: 0;
}

.login-node-right-bottom {
  right: 18px;
  bottom: 44px;
}

.login-node-bottom {
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
}

.login-node-left-bottom {
  bottom: 44px;
  left: 18px;
}

.login-node-left-middle {
  top: 222px;
  left: 0;
}

.login-node-left-top {
  top: 90px;
  left: 0;
}

.login-visual-title {
  position: relative;
  z-index: 2;
  text-align: center;
}

.login-visual-title div {
  color: #0b1536;
  font-size: 22px;
  font-weight: 800;
  line-height: 1.25;
}

.login-visual-title p {
  margin-top: 10px;
  color: rgba(11, 21, 54, 58%);
  font-size: 13px;
  line-height: 1.6;
}

.login-principles {
  position: relative;
  z-index: 2;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  border: 1px solid rgba(37, 99, 235, 10%);
  border-radius: 18px;
  padding: 12px;
  background: rgba(255, 255, 255, 58%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 58%);
}

.login-principle-item {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
}

.login-principle-item span {
  display: inline-flex;
  width: 30px;
  height: 30px;
  flex: 0 0 30px;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: rgba(37, 99, 235, 9%);
  color: #2563eb;
}

.login-principle-item div {
  display: flex;
  min-width: 0;
  flex-direction: column;
}

.login-principle-item strong {
  overflow: hidden;
  color: #2563eb;
  font-size: 11px;
  font-weight: 800;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.login-principle-item small {
  overflow: hidden;
  margin-top: 3px;
  color: rgba(11, 21, 54, 54%);
  font-size: 10px;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@keyframes login-core-float {
  0%,
  100% {
    transform: translate(-50%, -50%);
  }

  50% {
    transform: translate(-50%, calc(-50% - 8px));
  }
}

.dark {
  .login-left-bg {
    background:
      radial-gradient(circle at 50% 46%, rgba(37, 99, 235, 24%), transparent 36%),
      radial-gradient(circle at 70% 25%, rgba(124, 58, 237, 20%), transparent 30%),
      linear-gradient(135deg, #050914 0%, #02040a 58%, #071022 100%);
    border-right-color: rgba(255, 255, 255, 6%);
    color: #f8fbff;
  }

  .login-background {
    background:
      radial-gradient(circle at 50% 50%, rgba(37, 99, 235, 26%) 0%, transparent 58%),
      radial-gradient(circle at 30% 40%, rgba(124, 58, 237, 18%) 0%, transparent 46%),
      radial-gradient(circle at 72% 58%, rgba(0, 212, 255, 14%) 0%, transparent 48%);
    filter: blur(90px);
  }

  .login-grid {
    background-image:
      linear-gradient(rgba(37, 99, 235, 10%) 1px, transparent 1px),
      linear-gradient(90deg, rgba(37, 99, 235, 10%) 1px, transparent 1px);
  }

  .login-visual-inner {
    border-color: rgba(125, 211, 252, 10%);
    background:
      linear-gradient(180deg, rgba(15, 23, 42, 66%), rgba(2, 6, 23, 58%)),
      radial-gradient(circle at 50% 42%, rgba(37, 99, 235, 20%), transparent 48%);
    box-shadow:
      0 30px 90px rgba(0, 0, 0, 34%),
      inset 0 1px 0 rgba(255, 255, 255, 8%);
  }

  .login-visual-inner::before {
    border-color: rgba(125, 211, 252, 8%);
  }

  .login-visual-header {
    color: rgba(248, 251, 255, 56%);
  }

  .login-mode-pill {
    border-color: rgba(37, 99, 235, 24%);
    background: rgba(37, 99, 235, 12%);
    color: #7dd3fc;
    box-shadow: 0 10px 30px rgba(37, 99, 235, 22%);
  }

  .login-orbit {
    border-color: rgba(59, 130, 246, 28%);
    box-shadow: 0 0 34px rgba(37, 99, 235, 18%);
  }

  .login-visual-stage::before {
    background:
      radial-gradient(circle, rgba(37, 99, 235, 28%), transparent 62%),
      radial-gradient(circle, rgba(124, 58, 237, 20%), transparent 70%);
  }

  .login-visual-stage::after {
    background:
      linear-gradient(90deg, transparent 49%, rgba(125, 211, 252, 14%) 50%, transparent 51%),
      linear-gradient(0deg, transparent 49%, rgba(37, 99, 235, 14%) 50%, transparent 51%);
    opacity: 0.9;
  }

  .login-core-visual {
    border-color: rgba(125, 211, 252, 18%);
    background: rgba(2, 6, 23, 76%);
    box-shadow:
      0 28px 90px rgba(37, 99, 235, 26%),
      0 0 0 10px rgba(37, 99, 235, 8%);
  }

  .login-core-visual::after {
    border-color: rgba(125, 211, 252, 18%);
    box-shadow: inset 0 0 34px rgba(37, 99, 235, 26%);
  }

  .login-capability-node {
    border-color: rgba(125, 211, 252, 10%);
    background: rgba(15, 23, 42, 62%);
    color: rgba(248, 251, 255, 72%);
    box-shadow: 0 18px 46px rgba(0, 0, 0, 20%);
  }

  .login-node-icon {
    border-color: rgba(125, 211, 252, 16%);
    background: rgba(15, 23, 42, 78%);
    color: #7dd3fc;
    box-shadow: 0 14px 34px rgba(37, 99, 235, 24%);
  }

  .login-node-copy strong,
  .login-visual-title div {
    color: #f8fbff;
  }

  .login-node-copy small,
  .login-visual-title p {
    color: rgba(248, 251, 255, 58%);
  }

  .login-node-copy em {
    color: rgba(248, 251, 255, 46%);
  }

  .login-principles {
    border-top-color: rgba(125, 211, 252, 12%);
    border-color: rgba(125, 211, 252, 10%);
    background: rgba(15, 23, 42, 54%);
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 6%);
  }

  .login-principle-item span {
    background: rgba(37, 99, 235, 16%);
    color: #7dd3fc;
  }

  .login-principle-item strong {
    color: #7dd3fc;
  }

  .login-principle-item small {
    color: rgba(248, 251, 255, 52%);
  }
}

@media (max-height: 840px) {
  .login-visual-content {
    padding-top: 34px;
    padding-bottom: 28px;
  }

  .login-visual-inner {
    min-height: 560px;
  }

  .login-visual-stage {
    height: 430px;
  }

  .login-core-visual {
    width: 270px;
    height: 270px;
  }

  .login-orbit-outer {
    width: 490px;
    height: 292px;
  }

  .login-orbit-middle {
    width: 408px;
    height: 244px;
  }

  .login-orbit-inner {
    width: 300px;
    height: 188px;
  }

  .login-capability-node {
    width: 154px;
    min-height: 62px;
  }

  .login-node-icon {
    width: 36px;
    height: 36px;
    flex-basis: 36px;
  }

  .login-node-copy em {
    display: none;
  }
}

@media (max-width: 1280px) {
  .login-visual-content {
    padding-right: 48px;
    padding-left: 38px;
  }

  .login-capability-node {
    width: 148px;
  }

  .login-node-copy em {
    display: none;
  }
}

@media (max-width: 1440px) {
  .login-visual-content {
    padding-right: 36px;
    padding-left: 32px;
  }

  .login-visual-inner {
    width: min(100%, 620px);
  }
}

@media (max-width: 1180px) {
  .login-visual-content {
    padding: 32px 22px;
  }

  .login-visual-inner {
    width: min(100%, 500px);
    min-height: 560px;
    padding: 18px;
  }

  .login-visual-stage {
    height: 430px;
  }

  .login-core-visual {
    width: 270px;
    height: 270px;
  }

  .login-orbit-outer {
    width: 490px;
    height: 292px;
  }

  .login-orbit-middle {
    width: 408px;
    height: 244px;
  }

  .login-orbit-inner {
    width: 300px;
    height: 188px;
  }

  .login-capability-node {
    width: 136px;
    min-height: 58px;
    gap: 8px;
    padding: 8px;
  }

  .login-node-icon {
    width: 34px;
    height: 34px;
    flex-basis: 34px;
  }

  .login-node-copy strong {
    font-size: 12px;
  }

  .login-node-copy small {
    font-size: 10px;
  }

  .login-node-copy em {
    display: none;
  }
}

@media (max-width: 1120px) {
  .login-principles {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
