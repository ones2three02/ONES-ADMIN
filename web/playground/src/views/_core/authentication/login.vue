<script lang="ts" setup>
import type { VbenFormSchema } from '@vben/common-ui';
import type { Recordable } from '@vben/types';

import { computed, onMounted, ref } from 'vue';

import { AuthenticationLogin, z } from '@vben/common-ui';
import { useAppConfig } from '@vben/hooks';
import { IconifyIcon, SvgGoogleIcon } from '@vben/icons';
import { $t } from '@vben/locales';

import { message } from 'antdv-next';

import { getAuthProvidersApi, getOAuthAuthorizeApi } from '#/api';
import { useAuthStore } from '#/store';

defineOptions({ name: 'Login' });

const authStore = useAuthStore();
const {
  auth: { sso: ssoAuthConfig },
} = useAppConfig(import.meta.env, import.meta.env.PROD);
const feishuReady = ref(false);

const formSchema = computed((): VbenFormSchema[] => [
  {
    component: 'VbenInput',
    componentProps: {
      'aria-label': $t('authentication.username'),
      autocomplete: 'username',
      autofocus: true,
      placeholder: $t('authentication.usernameTip'),
    },
    fieldName: 'username',
    hideLabel: false,
    label: $t('authentication.username'),
    rules: z.string().min(1, { message: $t('authentication.usernameTip') }),
  },
  {
    component: 'VbenInputPassword',
    componentProps: {
      'aria-label': $t('authentication.password'),
      autocomplete: 'current-password',
      placeholder: $t('authentication.passwordTip'),
    },
    fieldName: 'password',
    hideLabel: false,
    label: $t('authentication.password'),
    rules: z.string().min(1, { message: $t('authentication.passwordTip') }),
  },
]);

function createLoginState(provider: string) {
  const randomId =
    globalThis.crypto?.randomUUID?.() ??
    `${Date.now()}-${Math.random().toString(16).slice(2)}`;
  return `${provider}_${randomId}`;
}

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

function handleGoogleLogin() {
  message.info($t('authentication.loginPanel.googleConfigTip'));
}

function getLoginMethodTitle(label: string, ready: boolean) {
  const status = ready
    ? $t('authentication.loginPanel.methodReadyTip')
    : $t('authentication.loginPanel.methodPendingTip');
  return `${label}，${status}`;
}

async function onSubmit(params: Recordable<any>) {
  await authStore.authLogin(params);
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
    class="ones-auth-login"
    data-testid="login-card"
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
      <header class="login-card-heading">
        <div class="login-card-brand">
          <span class="login-card-logo-frame">
            <img
              alt="ONES-ADMIN"
              decoding="async"
              fetchpriority="high"
              height="38"
              src="/brand/ones-brand-mark.png"
              width="38"
            />
          </span>
          <strong>ONES-ADMIN</strong>
        </div>
        <h2>{{ $t('authentication.loginPanel.title') }}</h2>
        <p>{{ $t('authentication.loginPanel.subtitle') }}</p>
        <IconifyIcon
          aria-hidden="true"
          class="login-field-icon login-field-icon--username"
          icon="lucide:user-round"
        />
        <IconifyIcon
          aria-hidden="true"
          class="login-field-icon login-field-icon--password"
          icon="lucide:lock-keyhole"
        />
      </header>
    </template>

    <template #third-party-login>
      <section class="login-methods" aria-labelledby="other-login-methods">
        <div class="login-method-title">
          <span></span>
          <em id="other-login-methods">
            {{ $t('authentication.loginPanel.otherMethods') }}
          </em>
          <span></span>
        </div>

        <div class="social-login-list" data-testid="social-login-list">
          <button
            :aria-label="$t('authentication.loginPanel.feishuShort')"
            class="social-login-button"
            :title="
              getLoginMethodTitle(
                $t('authentication.loginPanel.feishuShort'),
                feishuReady,
              )
            "
            type="button"
            @click="handleFeishuLogin"
          >
            <span class="social-login-icon social-login-icon--feishu">
              <img
                alt=""
                aria-hidden="true"
                src="/brand/feishu-login-icon.png"
              />
            </span>
            <span>{{ $t('authentication.loginPanel.feishuShort') }}</span>
          </button>

          <button
            :aria-label="$t('authentication.loginPanel.enterpriseSsoShort')"
            class="social-login-button"
            :title="
              getLoginMethodTitle(
                $t('authentication.loginPanel.enterpriseSsoShort'),
                Boolean(ssoAuthConfig?.url),
              )
            "
            type="button"
            @click="handleSsoLogin"
          >
            <span class="social-login-icon social-login-icon--wechat">
              <img
                alt=""
                aria-hidden="true"
                src="/brand/wecom-login-icon.png"
              />
            </span>
            <span>{{ $t('authentication.loginPanel.enterpriseSsoShort') }}</span>
          </button>

          <button
            :aria-label="$t('authentication.loginPanel.googleShort')"
            class="social-login-button"
            :title="
              getLoginMethodTitle(
                $t('authentication.loginPanel.googleShort'),
                false,
              )
            "
            type="button"
            @click="handleGoogleLogin"
          >
            <span class="social-login-icon social-login-icon--google">
              <SvgGoogleIcon />
            </span>
            <span>{{ $t('authentication.loginPanel.googleShort') }}</span>
          </button>
        </div>

        <div class="login-security-tip">
          <IconifyIcon aria-hidden="true" icon="lucide:shield-check" />
          <span>{{ $t('authentication.loginPanel.securityTip') }}</span>
        </div>
      </section>
    </template>
  </AuthenticationLogin>
</template>

<style scoped>
.ones-auth-login {
  position: relative;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.96);
  color: #1d2738;
}

.login-card-heading {
  margin-bottom: 26px;
}

.login-card-brand {
  display: flex;
  align-items: center;
  gap: 13px;
}

.login-card-logo-frame {
  display: inline-flex;
  width: 44px;
  height: 44px;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(222, 231, 246, 0.7);
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 8px 20px rgba(67, 113, 188, 0.1);
}

.login-card-logo-frame img {
  width: 32px;
  height: 32px;
  object-fit: contain;
}

.login-card-brand strong {
  color: #1b2537;
  font-size: 20px;
  font-weight: 800;
  letter-spacing: 0.01em;
}

.login-card-heading h2 {
  margin: 31px 0 6px;
  color: #172134;
  font-size: 29px;
  font-weight: 800;
  letter-spacing: 0.01em;
  line-height: 1.25;
}

.login-card-heading p {
  margin: 0;
  color: #8a96a8;
  font-size: 14px;
  font-weight: 500;
  line-height: 1.6;
}

.login-field-icon {
  position: absolute;
  left: 44px;
  z-index: 2;
  width: 17px;
  height: 17px;
  color: #b5bfce;
  pointer-events: none;
}

.login-field-icon--username {
  top: 241px;
}

.login-field-icon--password {
  top: 336px;
}

.ones-auth-login :deep([data-slot='form-item']) {
  align-items: stretch;
  flex-direction: column;
  padding-bottom: 19px;
}

.ones-auth-login :deep([data-slot='form-label']) {
  width: auto !important;
  align-self: stretch;
  justify-content: flex-start;
  margin: 0 0 12px;
  color: #3d485a;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.35;
}

.ones-auth-login :deep([data-slot='form-item'] > div) {
  overflow: visible;
  padding: 0;
}

.ones-auth-login :deep(input[name='username']),
.ones-auth-login :deep(input[name='password']) {
  height: 46px;
  border-color: #dbe2ed;
  border-radius: 10px;
  background: #fff;
  color: #253044;
  font-size: 14px;
  padding-left: 47px;
  box-shadow: 0 1px 2px rgba(37, 48, 68, 0.02);
}

.ones-auth-login :deep(input[name='username']:focus-visible),
.ones-auth-login :deep(input[name='password']:focus-visible) {
  border-color: #4d83ff;
  box-shadow: 0 0 0 3px rgba(37, 103, 255, 0.11);
}

.ones-auth-login :deep(form + div) {
  margin: 0 0 32px;
  color: #3f4b5d;
  font-size: 13px;
}

.ones-auth-login :deep(form + div .vben-link) {
  color: #2d73ff;
  font-size: 13px;
  font-weight: 600;
}

.ones-auth-login :deep([aria-label='login']) {
  height: 46px;
  border: 0;
  border-radius: 9px;
  background: linear-gradient(90deg, #2468f5 0%, #2c74fa 100%);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  box-shadow: 0 10px 20px rgba(40, 108, 247, 0.2);
}

.ones-auth-login :deep([aria-label='login']:hover) {
  background: linear-gradient(90deg, #1e60eb 0%, #246af0 100%);
}

.ones-auth-login :deep([aria-label='login']:focus-visible) {
  outline: 3px solid rgba(45, 115, 255, 0.25);
  outline-offset: 2px;
}

.login-methods {
  margin-top: 26px;
}

.login-method-title {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 14px;
}

.login-method-title span {
  height: 1px;
  background: #e8edf4;
}

.login-method-title em {
  color: #8b96a7;
  font-size: 13px;
  font-style: normal;
  font-weight: 500;
}

.social-login-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  margin-top: 17px;
}

.social-login-button {
  display: flex;
  min-width: 0;
  align-items: center;
  border: 0;
  padding: 0;
  background: transparent;
  color: #455164;
  cursor: pointer;
  flex-direction: column;
  font-size: 12px;
  gap: 7px;
  line-height: 1.2;
}

.social-login-icon {
  display: inline-flex;
  width: 38px;
  height: 38px;
  align-items: center;
  justify-content: center;
  border: 1px solid #e7ecf4;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 4px 12px rgba(64, 84, 117, 0.08);
  transition:
    border-color 160ms ease,
    box-shadow 160ms ease,
    transform 160ms ease;
}

.social-login-icon :deep(svg) {
  width: 21px;
  height: 21px;
}

.social-login-icon img {
  width: 24px;
  height: 24px;
  object-fit: contain;
}

.social-login-icon--feishu {
  color: #3370ff;
}

.social-login-icon--wechat {
  color: #16b15f;
}

.social-login-button:hover .social-login-icon {
  border-color: #cad8f4;
  box-shadow: 0 8px 18px rgba(65, 104, 176, 0.14);
  transform: translateY(-2px);
}

.social-login-button:focus-visible {
  border-radius: 10px;
  outline: 3px solid rgba(45, 115, 255, 0.22);
  outline-offset: 4px;
}

.login-security-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 37px;
  color: #a0aaba;
  font-size: 12px;
  font-weight: 500;
  gap: 6px;
  white-space: nowrap;
}

.login-security-tip :deep(svg) {
  width: 15px;
  height: 15px;
  color: #aab4c3;
}

@media (max-height: 900px) and (min-width: 1280px) {
  .login-card-heading {
    margin-bottom: 18px;
  }

  .login-card-heading h2 {
    margin-top: 18px;
  }

  .login-field-icon--username {
    top: 220px;
  }

  .login-field-icon--password {
    top: 308px;
  }

  .ones-auth-login :deep([data-slot='form-item']) {
    padding-bottom: 12px;
  }

  .login-methods {
    margin-top: 18px;
  }

  .login-security-tip {
    margin-top: 20px;
  }
}

@media (max-height: 800px) and (min-width: 1280px) {
  .login-field-icon--username {
    top: 214px;
  }

  .login-field-icon--password {
    top: 302px;
  }

  .ones-auth-login :deep([data-slot='form-item']) {
    padding-bottom: 10px;
  }

  .ones-auth-login :deep(form + div) {
    margin-bottom: 24px;
  }
}

@media (max-width: 640px) {
  .login-card-brand strong {
    font-size: 18px;
  }

  .login-card-heading h2 {
    font-size: 26px;
  }

  .login-security-tip {
    white-space: normal;
  }
}
</style>
