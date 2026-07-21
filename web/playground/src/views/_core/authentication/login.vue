<script lang="ts" setup>
import type { VbenFormSchema } from '@vben/common-ui';
import type { Recordable } from '@vben/types';

import { computed, markRaw, onMounted, ref, useTemplateRef } from 'vue';

import { AuthenticationLogin, SliderCaptcha, z } from '@vben/common-ui';
import { useAppConfig } from '@vben/hooks';
import { IconifyIcon } from '@vben/icons';
import { $t } from '@vben/locales';
import { usePreferences } from '@vben/preferences';

import { message } from 'antdv-next';

import { getAuthProvidersApi, getOAuthAuthorizeApi } from '#/api';
import { useAuthStore } from '#/store';

defineOptions({ name: 'Login' });

const authStore = useAuthStore();
const { isDark } = usePreferences();
const {
  auth: { sso: ssoAuthConfig },
} = useAppConfig(import.meta.env, import.meta.env.PROD);
const feishuReady = ref(false);
const loginBrandLogo = computed(() =>
  isDark.value
    ? '/brand/ones-1s-app-icon-dark.png'
    : '/brand/ones-1s-app-icon-light.png',
);


const formSchema = computed((): VbenFormSchema[] => {
  const schemas: VbenFormSchema[] = [
    {
      component: 'VbenInput',
      componentProps: {
        autocomplete: 'username',
        autofocus: true,
        placeholder: $t('authentication.usernameTip'),
      },
      fieldName: 'username',
      label: $t('authentication.username'),
      rules: z.string().min(1, { message: $t('authentication.usernameTip') }),
    },
    {
      component: 'VbenInputPassword',
      componentProps: {
        autocomplete: 'current-password',
        placeholder: $t('authentication.password'),
      },
      fieldName: 'password',
      label: $t('authentication.password'),
      rules: z.string().min(1, { message: $t('authentication.passwordTip') }),
    },
  ];

  if (import.meta.env.PROD) {
    schemas.push({
      component: markRaw(SliderCaptcha),
      fieldName: 'captcha',
      rules: z.boolean().refine((value) => value, {
        message: $t('authentication.verifyRequiredTip'),
      }),
    });
  }

  return schemas;
});

const loginRef =
  useTemplateRef<InstanceType<typeof AuthenticationLogin>>('loginRef');

function buildLoginUrlWithState(rawUrl: string, provider: string) {
  try {
    const url = new URL(rawUrl, window.location.origin);
    if (!url.searchParams.has('state')) {
      const state = createLoginState(provider);
      sessionStorage.setItem(`ONES_AUTH_STATE_${provider}`, state);
      url.searchParams.set('state', state);
    }
    return url.toString();
  } catch {
    return rawUrl;
  }
}

function createLoginState(provider: string) {
  const randomId =
    globalThis.crypto?.randomUUID?.() ??
    `${Date.now()}-${Math.random().toString(16).slice(2)}`;
  return `${provider}_${randomId}`;
}

async function handleFeishuLogin() {
  if (!feishuReady.value) {
    message.info($t('authentication.loginPanel.feishuConfigTip'));
    return;
  }
  const { authorizationUrl } = await getOAuthAuthorizeApi('feishu');
  window.location.href = authorizationUrl;
}

function handleSsoLogin() {
  if (!ssoAuthConfig?.url) {
    message.info($t('authentication.loginPanel.ssoConfigTip'));
    return;
  }
  window.location.href = buildLoginUrlWithState(ssoAuthConfig.url, 'sso');
}

function getLoginMethodTitle(label: string, ready: boolean) {
  const status = ready
    ? $t('authentication.loginPanel.methodReadyTip')
    : $t('authentication.loginPanel.methodPendingTip');
  return `${label}，${status}`;
}

async function onSubmit(params: Recordable<any>) {
  authStore.authLogin(params).catch(() => {
    // 登陆失败后刷新验证码
    const formApi = loginRef.value?.getFormApi();
    // 重置验证码组件的值
    formApi?.setFieldValue('captcha', false, false);
    // 使用表单API获取验证码组件实例，并调用其resume方法来重置验证码
    formApi
      ?.getFieldComponentRef<InstanceType<typeof SliderCaptcha>>('captcha')
      ?.resume();
  });
}

onMounted(async () => {
  try {
    const providers = await getAuthProvidersApi();
    feishuReady.value = Boolean(
      providers.find((provider) => provider.id === 'feishu')?.enabled,
    );
  } catch {
    feishuReady.value = false;
  }
});
</script>

<template>
  <AuthenticationLogin
    ref="loginRef"
    class="ones-auth-login"
    :form-schema="formSchema"
    :loading="authStore.loginLoading"
    :show-code-login="false"
    :show-qrcode-login="false"
    :show-register="false"
    :show-third-party-login="false"
    :submit-button-text="$t('authentication.loginPanel.submitText')"
    @submit="onSubmit"
  >
    <template #title>
      <RouterLink
        :aria-label="$t('authentication.loginPanel.qrcodeSwitchTip')"
        class="enterprise-login-qrcode-corner"
        :title="$t('authentication.loginPanel.qrcodeSwitchTip')"
        to="/auth/qrcode-login"
      >
        <IconifyIcon icon="lucide:qr-code" />
      </RouterLink>

      <div class="enterprise-login-heading">
        <div class="enterprise-login-brand">
          <span class="enterprise-login-logo-frame">
            <img
              alt="ONES SYSTEM"
              decoding="async"
              fetchpriority="high"
              height="46"
              :src="loginBrandLogo"
              width="46"
            />
          </span>
          <div>
            <strong>ONES-ADMIN</strong>
          </div>
        </div>
        <h2>{{ $t('authentication.loginPanel.title') }}</h2>
      </div>
    </template>

    <template #third-party-login>
      <div class="enterprise-login-methods">
        <div class="enterprise-login-method-title">
          <span></span>
          <em>{{ $t('authentication.loginPanel.otherMethods') }}</em>
          <span></span>
        </div>

        <div class="enterprise-login-icon-row">
          <button
            :aria-label="
              getLoginMethodTitle(
                $t('authentication.feishuLogin'),
                feishuReady,
              )
            "
            class="enterprise-login-icon-button enterprise-login-icon-button-primary"
            :data-ready="feishuReady"
            :title="
              getLoginMethodTitle(
                $t('authentication.feishuLogin'),
                feishuReady,
              )
            "
            type="button"
            @click="handleFeishuLogin"
          >
            <IconifyIcon icon="lucide:message-square-more" />
          </button>

          <button
            :aria-label="
              getLoginMethodTitle(
                $t('authentication.loginPanel.enterpriseSsoShort'),
                Boolean(ssoAuthConfig?.url),
              )
            "
            class="enterprise-login-icon-button"
            :data-ready="Boolean(ssoAuthConfig?.url)"
            :title="
              getLoginMethodTitle(
                $t('authentication.loginPanel.enterpriseSsoShort'),
                Boolean(ssoAuthConfig?.url),
              )
            "
            type="button"
            @click="handleSsoLogin"
          >
            <IconifyIcon icon="lucide:building-2" />
          </button>

          <RouterLink
            :aria-label="
              getLoginMethodTitle($t('authentication.mobileLogin'), false)
            "
            class="enterprise-login-icon-button"
            data-ready="false"
            :title="
              getLoginMethodTitle($t('authentication.mobileLogin'), false)
            "
            to="/auth/code-login"
          >
            <IconifyIcon icon="lucide:smartphone" />
          </RouterLink>
        </div>
      </div>
    </template>
  </AuthenticationLogin>
</template>

<style scoped>
.ones-auth-login {
  position: relative;
}

.enterprise-login-qrcode-corner {
  position: absolute;
  top: 0;
  right: 0;
  z-index: 2;
  display: inline-flex;
  width: 62px;
  height: 62px;
  align-items: flex-end;
  justify-content: flex-start;
  border-radius: 0 22px 0 18px;
  padding: 0 0 13px 13px;
  background:
    linear-gradient(
      135deg,
      hsl(var(--primary) / 14%),
      hsl(var(--background) / 84%)
    ),
    hsl(var(--background));
  color: hsl(var(--primary));
  box-shadow:
    inset 1px -1px 0 hsl(var(--border) / 72%),
    0 12px 28px hsl(var(--primary) / 10%);
  cursor: pointer;
  transition:
    background 0.18s ease,
    color 0.18s ease,
    transform 0.18s ease;
}

.enterprise-login-qrcode-corner::after {
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: linear-gradient(
    135deg,
    transparent 48%,
    hsl(var(--border) / 68%) 49%,
    transparent 51%
  );
  pointer-events: none;
  content: '';
}

.enterprise-login-qrcode-corner:hover {
  color: hsl(var(--primary));
  transform: translate(-1px, 1px);
}

.enterprise-login-qrcode-corner:focus-visible {
  outline: 2px solid hsl(var(--primary));
  outline-offset: 3px;
}

.enterprise-login-qrcode-corner svg {
  width: 20px;
  height: 20px;
}

.enterprise-login-heading {
  margin-bottom: 24px;
}

.enterprise-login-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
}

.enterprise-login-logo-frame {
  display: inline-flex;
  width: 46px;
  height: 46px;
  flex: 0 0 46px;
  border-radius: 12px;
  background: hsl(var(--background));
  box-shadow:
    0 16px 36px hsl(var(--primary) / 18%),
    0 0 0 1px hsl(var(--border));
  overflow: hidden;
}

.enterprise-login-logo-frame img {
  width: 46px;
  height: 46px;
  object-fit: cover;
  transform: translateZ(0);
}

.enterprise-login-brand div {
  display: flex;
  min-width: 0;
  flex-direction: column;
}

.enterprise-login-brand strong {
  color: hsl(var(--foreground));
  font-size: 18px;
  font-weight: 900;
  letter-spacing: 0;
  line-height: 1.1;
}

.enterprise-login-heading h2 {
  margin: 0;
  color: hsl(var(--foreground));
  font-size: 34px;
  font-weight: 900;
  letter-spacing: 0;
  line-height: 1.08;
}

.ones-auth-login :deep(form) {
  margin-top: 4px;
}

.ones-auth-login :deep(input) {
  height: 46px;
  border-color: hsl(var(--border) / 82%);
  border-radius: 10px;
  background:
    linear-gradient(
      180deg,
      hsl(var(--background) / 78%),
      hsl(var(--background) / 58%)
    ),
    hsl(var(--background));
  box-shadow:
    inset 0 1px 0 hsl(var(--foreground) / 4%),
    0 10px 24px hsl(var(--foreground) / 3%);
}

.ones-auth-login :deep(input:focus) {
  border-color: hsl(var(--primary) / 52%);
  box-shadow:
    0 0 0 3px hsl(var(--primary) / 12%),
    inset 0 1px 0 hsl(var(--foreground) / 4%);
}

.ones-auth-login :deep(button[aria-label='login']) {
  height: 46px;
  border-radius: 10px;
  background:
    linear-gradient(135deg, #2563eb 0%, #3656f1 48%, #00a6ff 100%),
    hsl(var(--primary));
  color: white;
  font-weight: 800;
  box-shadow:
    0 16px 34px hsl(var(--primary) / 28%),
    inset 0 1px 0 rgba(255, 255, 255, 24%);
}

.ones-auth-login :deep(button[aria-label='login']:hover) {
  filter: brightness(1.04);
}

.ones-auth-login :deep(button:not([aria-label='login'])) {
  border-radius: 10px;
}

.enterprise-login-methods {
  margin-top: 20px;
}

.enterprise-login-method-title {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.enterprise-login-method-title span {
  height: 1px;
  background: linear-gradient(
    90deg,
    transparent,
    hsl(var(--border)),
    transparent
  );
}

.enterprise-login-method-title em {
  color: hsl(var(--muted-foreground));
  font-size: 12px;
  font-style: normal;
  font-weight: 700;
  letter-spacing: 0;
}

.enterprise-login-icon-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
}

.enterprise-login-icon-button {
  position: relative;
  display: inline-flex;
  width: 42px;
  height: 42px;
  align-items: center;
  justify-content: center;
  border: 1px solid hsl(var(--border) / 82%);
  border-radius: 999px;
  background:
    linear-gradient(
      180deg,
      hsl(var(--background) / 84%),
      hsl(var(--background) / 66%)
    ),
    hsl(var(--background));
  color: hsl(var(--foreground) / 82%);
  cursor: pointer;
  outline: none;
  text-decoration: none;
  box-shadow:
    inset 0 1px 0 hsl(var(--foreground) / 4%),
    0 10px 22px hsl(var(--foreground) / 3%);
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    color 0.18s ease,
    transform 0.18s ease;
}

.enterprise-login-icon-button:hover {
  border-color: hsl(var(--primary) / 42%);
  color: hsl(var(--primary));
  box-shadow:
    0 0 0 3px hsl(var(--primary) / 9%),
    0 12px 24px hsl(var(--primary) / 10%);
  transform: translateY(-1px);
}

.enterprise-login-icon-button:focus-visible {
  outline: 2px solid hsl(var(--primary));
  outline-offset: 3px;
}

.ones-auth-login .enterprise-login-methods .enterprise-login-icon-button {
  border-radius: 999px;
}

.enterprise-login-icon-button-primary {
  color: hsl(var(--primary));
}

.enterprise-login-icon-button[data-ready='false'] {
  color: hsl(var(--foreground) / 58%);
}

.enterprise-login-icon-button[data-ready='false']::after {
  position: absolute;
  top: 5px;
  right: 5px;
  width: 7px;
  height: 7px;
  border: 2px solid hsl(var(--background));
  border-radius: 999px;
  background: hsl(var(--warning));
  box-shadow: 0 0 0 2px hsl(var(--warning) / 16%);
  content: '';
}

.enterprise-login-icon-button[data-ready='false']:hover {
  border-color: hsl(var(--warning) / 52%);
  color: hsl(var(--foreground) / 76%);
  box-shadow:
    0 0 0 3px hsl(var(--warning) / 10%),
    0 12px 24px hsl(var(--warning) / 10%);
}

.enterprise-login-icon-button svg {
  width: 18px;
  height: 18px;
}

@media (max-width: 480px) {
  .enterprise-login-heading h2 {
    font-size: 30px;
  }

  .enterprise-login-qrcode-corner {
    top: 0;
    right: 0;
  }
}
</style>
