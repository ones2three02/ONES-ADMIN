<script setup lang="ts">
import type { ToolbarType } from './types';

import { computed } from 'vue';

import { preferences, usePreferences } from '@vben/preferences';

import { VbenIcon } from '@vben-core/shadcn-ui';

import { Copyright } from '../basic/copyright';
import AuthenticationFormView from './form.vue';
import OnesVisualStage from './ones-visual-stage.vue';
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
  sloganImageDark?: string;
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
  sloganImageDark: '',
  toolbar: true,
  toolbarList: () => ['color', 'language', 'layout', 'theme'],
  clickLogo: () => {},
});

const { authPanelCenter, authPanelLeft, authPanelRight, isDark } =
  usePreferences();

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
          <p v-if="appName" class="m-0 text-xl font-bold tracking-tight">
            {{ appName }}
          </p>
        </div>
      </div>
    </slot>

    <!-- 系统介绍 (左侧 62% 视觉面板) -->
    <div v-if="!authPanelCenter" class="relative hidden w-0 flex-1 xl:block xl:w-[62%]">
      <div
        class="absolute inset-0 size-full bg-background-deep dark:bg-[#0a1020] login-left-bg"
      >
        <div class="login-background absolute top-0 left-0 size-full"></div>
        <div class="login-grid absolute inset-0"></div>
        <div
          :key="authPanelLeft ? 'left' : authPanelRight ? 'right' : 'center'"
          class="login-visual-content flex flex-col justify-between h-full p-10 z-10 relative"
          :class="{
            'enter-x': authPanelLeft,
            '-enter-x': authPanelRight,
          }"
        >
          <!-- 头部标题标语与 4 大能力卡片 -->
          <div class="space-y-6 pt-4 max-w-[760px]">
            <div>
              <span class="inline-block px-3 py-1 text-sm font-extrabold text-[#246bfe] bg-blue-50/90 dark:bg-blue-950/60 dark:text-blue-400 rounded-full border border-blue-200/60 dark:border-blue-800/40 mb-3 tracking-wider">
                ONES / 1S
              </span>
              <h1 class="text-4xl xl:text-5xl font-black text-foreground tracking-tight leading-[1.08]">
                新一代企业协同管理平台
              </h1>
              <p class="text-base xl:text-lg text-muted-foreground mt-3 font-normal">
                连接人、流程与数据，助力企业高效运营与持续增长
              </p>
            </div>

            <!-- 4 大能力卡片 Badge 布局 -->
            <div class="grid grid-cols-4 gap-4 pt-2 max-w-[790px]">
              <div class="flex items-center space-x-3 p-3 rounded-2xl bg-white/80 dark:bg-slate-900/60 border border-blue-100/80 dark:border-slate-800 shadow-sm backdrop-blur-md">
                <div class="p-2.5 rounded-xl bg-blue-50 dark:bg-blue-950 text-[#246bfe] dark:text-blue-400 flex items-center justify-center shrink-0">
                  <VbenIcon icon="lucide:sparkles" class="size-5" />
                </div>
                <div class="min-w-0">
                  <div class="text-sm font-bold text-foreground truncate">高效协同</div>
                  <div class="text-xs text-muted-foreground truncate mt-0.5">提升团队效率</div>
                </div>
              </div>

              <div class="flex items-center space-x-3 p-3 rounded-2xl bg-white/80 dark:bg-slate-900/60 border border-blue-100/80 dark:border-slate-800 shadow-sm backdrop-blur-md">
                <div class="p-2.5 rounded-xl bg-blue-50 dark:bg-blue-950 text-[#246bfe] dark:text-blue-400 flex items-center justify-center shrink-0">
                  <VbenIcon icon="lucide:workflow" class="size-5" />
                </div>
                <div class="min-w-0">
                  <div class="text-sm font-bold text-foreground truncate">流程驱动</div>
                  <div class="text-xs text-muted-foreground truncate mt-0.5">优化业务流程</div>
                </div>
              </div>

              <div class="flex items-center space-x-3 p-3 rounded-2xl bg-white/80 dark:bg-slate-900/60 border border-blue-100/80 dark:border-slate-800 shadow-sm backdrop-blur-md">
                <div class="p-2.5 rounded-xl bg-blue-50 dark:bg-blue-950 text-[#246bfe] dark:text-blue-400 flex items-center justify-center shrink-0">
                  <VbenIcon icon="lucide:trending-up" class="size-5" />
                </div>
                <div class="min-w-0">
                  <div class="text-sm font-bold text-foreground truncate">数据洞察</div>
                  <div class="text-xs text-muted-foreground truncate mt-0.5">辅助决策分析</div>
                </div>
              </div>

              <div class="flex items-center space-x-3 p-3 rounded-2xl bg-white/80 dark:bg-slate-900/60 border border-blue-100/80 dark:border-slate-800 shadow-sm backdrop-blur-md">
                <div class="p-2.5 rounded-xl bg-blue-50 dark:bg-blue-950 text-[#246bfe] dark:text-blue-400 flex items-center justify-center shrink-0">
                  <VbenIcon icon="lucide:shield-check" class="size-5" />
                </div>
                <div class="min-w-0">
                  <div class="text-sm font-bold text-foreground truncate">安全可靠</div>
                  <div class="text-xs text-muted-foreground truncate mt-0.5">企业级安全防护</div>
                </div>
              </div>
            </div>
          </div>

          <!-- 中央 3D 生态 6 大架构图 -->
          <div class="my-auto py-2 relative w-full h-[380px]">
            <OnesVisualStage />
          </div>

          <!-- 底部 4 大数据指标统计栏 -->
          <div class="grid grid-cols-4 gap-4 p-4 rounded-2xl bg-white/75 dark:bg-slate-900/70 border border-blue-100/80 dark:border-slate-800 shadow-lg backdrop-blur-xl mb-2">
            <div class="text-center relative after:content-[''] after:absolute after:right-0 after:top-[24%] after:h-[52%] after:w-[1px] after:bg-blue-100 dark:after:bg-slate-800">
              <div class="text-2xl xl:text-3xl font-black text-foreground">20+</div>
              <div class="text-xs text-muted-foreground font-medium mt-1">业务模块</div>
            </div>
            <div class="text-center relative after:content-[''] after:absolute after:right-0 after:top-[24%] after:h-[52%] after:w-[1px] after:bg-blue-100 dark:after:bg-slate-800">
              <div class="text-2xl xl:text-3xl font-black text-foreground">100+</div>
              <div class="text-xs text-muted-foreground font-medium mt-1">企业服务</div>
            </div>
            <div class="text-center relative after:content-[''] after:absolute after:right-0 after:top-[24%] after:h-[52%] after:w-[1px] after:bg-blue-100 dark:after:bg-slate-800">
              <div class="text-2xl xl:text-3xl font-black text-foreground">10W+</div>
              <div class="text-xs text-muted-foreground font-medium mt-1">用户信任</div>
            </div>
            <div class="text-center">
              <div class="text-2xl xl:text-3xl font-black text-foreground">99.9%</div>
              <div class="text-xs text-muted-foreground font-medium mt-1">系统可用性</div>
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

    <!-- 右侧认证面板 (38% 宽度) -->
    <AuthenticationFormView
      v-if="authPanelRight"
      class="min-h-full w-full xl:w-[38%]"
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
  overflow: hidden;
  background:
    radial-gradient(circle at 50% 42%, rgba(37, 99, 235, 22%), transparent 36%),
    radial-gradient(circle at 78% 18%, rgba(99, 102, 241, 14%), transparent 30%),
    radial-gradient(circle at 20% 80%, rgba(6, 182, 212, 11%), transparent 32%),
    linear-gradient(135deg, #fdfeff 0%, #f6faff 44%, #edf6ff 100%);
  border-right: 1px solid rgba(37, 99, 235, 10%);
  color: #0b1536;
}

.login-background {
  background:
    radial-gradient(circle at 48% 48%, rgba(37, 99, 235, 24%) 0%, transparent 57%),
    radial-gradient(circle at 32% 34%, rgba(124, 58, 237, 15%) 0%, transparent 44%),
    radial-gradient(circle at 70% 58%, rgba(6, 182, 212, 15%) 0%, transparent 46%);
  filter: blur(82px);
}

.login-grid {
  background-image:
    linear-gradient(rgba(37, 99, 235, 5%) 1px, transparent 1px),
    linear-gradient(90deg, rgba(37, 99, 235, 5%) 1px, transparent 1px);
  background-size: 32px 32px;
  opacity: 0.72;
  mask-image: radial-gradient(circle at 50% 48%, black 0%, black 58%, transparent 82%);
}

.login-visual-content {
  padding: 34px 48px 30px 42px;
}

.login-visual-inner {
  position: relative;
  z-index: 1;
  display: flex;
  box-sizing: border-box;
  width: min(100%, 880px);
  height: min(88vh, 780px);
  min-height: 650px;
  flex-direction: column;
  justify-content: space-between;
  overflow: hidden;
  border: 1px solid rgba(37, 99, 235, 10%);
  border-radius: 24px;
  padding: 22px 32px 24px;
  background:
    radial-gradient(circle at 50% 45%, rgba(37, 99, 235, 13%), transparent 48%),
    linear-gradient(180deg, rgba(255, 255, 255, 78%), rgba(255, 255, 255, 46%));
  box-shadow:
    0 34px 96px rgba(37, 99, 235, 12%),
    inset 0 1px 0 rgba(255, 255, 255, 76%);
  backdrop-filter: blur(20px);
}

.login-visual-inner::before {
  position: absolute;
  inset: 16px;
  border: 1px solid rgba(37, 99, 235, 8%);
  border-radius: 24px;
  pointer-events: none;
  content: '';
}

.login-visual-inner::after {
  position: absolute;
  inset: auto 8% 74px;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(37, 99, 235, 24%), transparent);
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

.login-visual-brand {
  display: flex;
  margin-left: auto;
  flex-direction: column;
  align-items: flex-end;
  color: rgba(11, 21, 54, 62%);
  line-height: 1.2;
}

.login-visual-brand strong {
  color: #0b1536;
  font-size: 14px;
  font-weight: 800;
}

.login-visual-brand small {
  margin-top: 3px;
  color: rgba(11, 21, 54, 42%);
  font-size: 10px;
  font-weight: 700;
}

.login-visual-stage {
  position: relative;
  isolation: isolate;
  height: 520px;
  margin-top: 2px;
  overflow: hidden;
}

.login-visual-stage::before {
  position: absolute;
  top: 55%;
  left: 50%;
  z-index: -1;
  width: 560px;
  height: 202px;
  border-radius: 50%;
  background:
    radial-gradient(circle, rgba(37, 99, 235, 24%), transparent 62%),
    radial-gradient(circle, rgba(6, 182, 212, 18%), transparent 70%),
    linear-gradient(90deg, transparent, rgba(124, 58, 237, 18%), transparent);
  filter: blur(8px);
  transform: translate(-50%, -50%) rotate(-7deg);
  content: '';
}

.login-visual-stage::after {
  position: absolute;
  top: 50%;
  left: 50%;
  z-index: -1;
  width: 560px;
  height: 376px;
  border-radius: 50%;
  background:
    linear-gradient(90deg, transparent 49.6%, rgba(37, 99, 235, 16%) 50%, transparent 50.4%),
    linear-gradient(0deg, transparent 49.6%, rgba(37, 99, 235, 10%) 50%, transparent 50.4%);
  opacity: 0.72;
  transform: translate(-50%, -50%) rotate(-9deg);
  content: '';
}

.login-axis {
  position: absolute;
  top: 50%;
  left: 50%;
  z-index: -1;
  background: linear-gradient(90deg, transparent, rgba(37, 99, 235, 20%), transparent);
  opacity: 0.72;
  pointer-events: none;
}

.login-axis-x {
  width: 620px;
  height: 1px;
  transform: translate(-50%, -50%);
}

.login-axis-y {
  width: 1px;
  height: 390px;
  transform: translate(-50%, -50%) rotate(180deg);
}

.login-orbit {
  position: absolute;
  inset: 50%;
  border: 1px solid rgba(37, 99, 235, 24%);
  border-style: dashed;
  border-radius: 50%;
  box-shadow: 0 0 34px rgba(37, 99, 235, 11%);
  animation: login-orbit-breathe 8s ease-in-out infinite;
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
  top: 16%;
  right: 14%;
}

.login-orbit::after {
  bottom: 15%;
  left: 20%;
}

.login-orbit-outer {
  width: 610px;
  height: 356px;
  transform: translate(-50%, -50%) rotate(-9deg);
}

.login-orbit-middle {
  width: 510px;
  height: 300px;
  opacity: 0.78;
  transform: translate(-50%, -50%) rotate(12deg);
}

.login-orbit-inner {
  width: 382px;
  height: 232px;
  opacity: 0.66;
  transform: translate(-50%, -50%) rotate(-23deg);
}

.login-scan-line {
  position: absolute;
  top: 10%;
  left: 50%;
  width: 1px;
  height: 80%;
  background: linear-gradient(transparent, rgba(37, 99, 235, 46%), transparent);
  box-shadow: 0 0 18px rgba(37, 99, 235, 28%);
  transform: translateX(-50%);
}

.login-core-visual {
  position: absolute;
  top: 50%;
  left: 50%;
  z-index: 1;
  width: 454px;
  height: 390px;
  overflow: hidden;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  animation: login-core-float 5s ease-in-out infinite;
  mask-image: radial-gradient(ellipse at 50% 55%, black 0%, black 52%, rgba(0, 0, 0, 72%) 64%, transparent 80%);
  transform: translate(-50%, -50%);
}

.login-core-visual::before {
  position: absolute;
  top: 64%;
  left: 50%;
  z-index: 1;
  width: 76%;
  height: 24%;
  border-radius: 50%;
  background:
    radial-gradient(circle, rgba(37, 99, 235, 30%), transparent 64%),
    radial-gradient(circle, rgba(6, 182, 212, 18%), transparent 70%);
  filter: blur(14px);
  transform: translate(-50%, -50%);
  content: '';
}

.login-core-visual::after {
  position: absolute;
  top: 12%;
  left: 50%;
  z-index: -1;
  width: 76%;
  height: 72%;
  border-radius: 999px;
  background: radial-gradient(circle, rgba(37, 99, 235, 18%), transparent 66%);
  filter: blur(34px);
  pointer-events: none;
  transform: translateX(-50%);
  content: '';
}

.login-core-visual img,
.login-core-visual svg {
  width: 100%;
  height: 100%;
  object-fit: contain;
  filter: drop-shadow(0 34px 52px rgba(37, 99, 235, 24%));
}

.login-capability-node {
  position: absolute;
  z-index: 2;
  display: flex;
  width: 186px;
  min-height: 74px;
  align-items: flex-start;
  gap: 12px;
  border: 0;
  border-radius: 0;
  padding: 0;
  background: transparent;
  color: rgba(11, 21, 54, 74%);
  box-shadow: none;
  text-shadow: 0 1px 0 rgba(255, 255, 255, 48%);
}

.login-node-icon {
  display: inline-flex;
  width: 48px;
  height: 48px;
  flex: 0 0 48px;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(37, 99, 235, 14%);
  border-radius: 999px;
  background:
    radial-gradient(circle at 38% 28%, rgba(255, 255, 255, 92%), rgba(255, 255, 255, 66%)),
    rgba(255, 255, 255, 74%);
  color: #2563eb;
  box-shadow:
    0 18px 38px rgba(37, 99, 235, 18%),
    0 0 0 8px rgba(37, 99, 235, 5%),
    inset 0 0 18px rgba(37, 99, 235, 8%);
}

.login-node-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
}

.login-node-copy strong {
  color: #0b1536;
  font-size: 15px;
  font-weight: 800;
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
  line-height: 1.45;
}

.login-node-top {
  top: 4px;
  left: 50%;
  transform: translateX(-50%);
}

.login-node-right-top {
  top: 88px;
  right: 6px;
}

.login-node-right-middle {
  top: 226px;
  right: 0;
}

.login-node-right-bottom {
  right: 28px;
  bottom: 42px;
}

.login-node-bottom {
  bottom: 4px;
  left: 50%;
  transform: translateX(-50%);
}

.login-node-left-bottom {
  bottom: 42px;
  left: 28px;
}

.login-node-left-middle {
  top: 226px;
  left: 0;
}

.login-node-left-top {
  top: 88px;
  left: 6px;
}

.login-visual-title {
  position: relative;
  z-index: 2;
  margin-top: -4px;
  text-align: center;
}

.login-visual-title div {
  color: #0b1536;
  font-size: 23px;
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
  gap: 10px;
  border: 1px solid rgba(37, 99, 235, 10%);
  border-radius: 16px;
  padding: 10px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 62%), rgba(255, 255, 255, 36%)),
    rgba(255, 255, 255, 44%);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 58%),
    0 16px 46px rgba(37, 99, 235, 8%);
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
  border: 1px solid rgba(37, 99, 235, 10%);
  border-radius: 999px;
  background: rgba(255, 255, 255, 58%);
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

@keyframes login-orbit-breathe {
  0%,
  100% {
    opacity: 0.84;
  }

  50% {
    opacity: 0.58;
  }
}

.dark {
  .login-left-bg {
    background:
      radial-gradient(circle at 50% 46%, rgba(37, 99, 235, 30%), transparent 36%),
      radial-gradient(circle at 70% 22%, rgba(124, 58, 237, 22%), transparent 31%),
      radial-gradient(circle at 20% 78%, rgba(6, 182, 212, 10%), transparent 32%),
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
      linear-gradient(rgba(37, 99, 235, 9%) 1px, transparent 1px),
      linear-gradient(90deg, rgba(37, 99, 235, 9%) 1px, transparent 1px);
  }

  .login-visual-inner {
    border-color: rgba(125, 211, 252, 11%);
    background:
      radial-gradient(circle at 50% 43%, rgba(37, 99, 235, 24%), transparent 47%),
      linear-gradient(180deg, rgba(15, 23, 42, 58%), rgba(2, 6, 23, 42%));
    box-shadow:
      0 34px 96px rgba(0, 0, 0, 36%),
      inset 0 1px 0 rgba(255, 255, 255, 8%);
  }

  .login-visual-inner::before {
    border-color: rgba(125, 211, 252, 8%);
  }

  .login-visual-header {
    color: rgba(248, 251, 255, 56%);
  }

  .login-visual-brand {
    color: rgba(248, 251, 255, 54%);
  }

  .login-visual-brand strong {
    color: #f8fbff;
  }

  .login-visual-brand small {
    color: rgba(248, 251, 255, 38%);
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

  .login-axis {
    background: linear-gradient(90deg, transparent, rgba(125, 211, 252, 18%), transparent);
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
    background: transparent;
    box-shadow: none;
  }

  .login-core-visual::after {
    background: radial-gradient(circle, rgba(37, 99, 235, 24%), transparent 66%);
  }

  .login-capability-node {
    color: rgba(248, 251, 255, 72%);
    text-shadow: 0 0 18px rgba(37, 99, 235, 16%);
  }

  .login-node-icon {
    border-color: rgba(125, 211, 252, 16%);
    background:
      radial-gradient(circle at 35% 25%, rgba(37, 99, 235, 38%), rgba(15, 23, 42, 76%)),
      rgba(15, 23, 42, 78%);
    color: #7dd3fc;
    box-shadow:
      0 16px 40px rgba(37, 99, 235, 26%),
      0 0 0 8px rgba(37, 99, 235, 9%);
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
    background: rgba(15, 23, 42, 42%);
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 6%);
  }

  .login-principle-item span {
    border-color: rgba(125, 211, 252, 12%);
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
    padding-top: 28px;
    padding-bottom: 24px;
  }

  .login-visual-inner {
    min-height: 580px;
  }

  .login-visual-stage {
    height: 438px;
  }

  .login-core-visual {
    width: 382px;
    height: 326px;
  }

  .login-orbit-outer {
    width: 548px;
    height: 318px;
  }

  .login-orbit-middle {
    width: 462px;
    height: 270px;
  }

  .login-orbit-inner {
    width: 346px;
    height: 210px;
  }

  .login-capability-node {
    width: 162px;
    min-height: 62px;
  }

  .login-node-icon {
    width: 40px;
    height: 40px;
    flex-basis: 40px;
  }

  .login-node-copy em {
    display: none;
  }
}

@media (max-width: 1280px) {
  .login-visual-content {
    padding-right: 36px;
    padding-left: 30px;
  }

  .login-capability-node {
    width: 154px;
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
    width: min(100%, 720px);
  }
}

@media (max-width: 1180px) {
  .login-visual-content {
    padding: 32px 22px;
  }

  .login-visual-inner {
    width: min(100%, 560px);
    min-height: 570px;
    padding: 18px;
  }

  .login-visual-stage {
    height: 430px;
  }

  .login-core-visual {
    width: 330px;
    height: 286px;
  }

  .login-orbit-outer {
    width: 496px;
    height: 288px;
  }

  .login-orbit-middle {
    width: 414px;
    height: 242px;
  }

  .login-orbit-inner {
    width: 312px;
    height: 190px;
  }

  .login-capability-node {
    width: 138px;
    min-height: 58px;
    gap: 8px;
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

@media (prefers-reduced-motion: reduce) {
  .login-core-visual,
  .login-orbit {
    animation: none;
  }
}
</style>
