<script lang="ts" setup>
import type { VbenFormSchema } from '@vben/common-ui';
import type { Recordable } from '@vben/types';

import { computed, markRaw, useTemplateRef } from 'vue';

import { AuthenticationLogin, SliderCaptcha, z } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { $t } from '@vben/locales';

import { useAuthStore } from '#/store';

defineOptions({ name: 'Login' });

const authStore = useAuthStore();

const formSchema = computed((): VbenFormSchema[] => {
  return [
    {
      component: 'VbenInput',
      componentProps: {
        placeholder: $t('authentication.usernameTip'),
      },
      fieldName: 'username',
      label: $t('authentication.username'),
      rules: z.string().min(1, { message: $t('authentication.usernameTip') }),
    },
    {
      component: 'VbenInputPassword',
      componentProps: {
        placeholder: $t('authentication.password'),
      },
      fieldName: 'password',
      label: $t('authentication.password'),
      rules: z.string().min(1, { message: $t('authentication.passwordTip') }),
    },
    {
      component: markRaw(SliderCaptcha),
      fieldName: 'captcha',
      rules: z.boolean().refine((value) => value, {
        message: $t('authentication.verifyRequiredTip'),
      }),
    },
  ];
});

const loginRef =
  useTemplateRef<InstanceType<typeof AuthenticationLogin>>('loginRef');

const enterpriseStatusItems = computed(() => [
  {
    description: $t('authentication.loginPanel.authDescription'),
    icon: 'lucide:key-round',
    title: $t('authentication.loginPanel.authTitle'),
  },
  {
    description: $t('authentication.loginPanel.lockDescription'),
    icon: 'lucide:shield-alert',
    title: $t('authentication.loginPanel.lockTitle'),
  },
  {
    description: $t('authentication.loginPanel.auditDescription'),
    icon: 'lucide:scan-search',
    title: $t('authentication.loginPanel.auditTitle'),
  },
]);

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
</script>

<template>
  <AuthenticationLogin
    ref="loginRef"
    :form-schema="formSchema"
    :loading="authStore.loginLoading"
    :show-register="false"
    :show-third-party-login="false"
    @submit="onSubmit"
  >
    <template #title>
      <div class="enterprise-login-heading">
        <div class="enterprise-login-brand">
          <img alt="ONES SYSTEM" src="/brand/ones-1s-app-icon-192.png" />
          <div>
            <strong>ONES-ADMIN</strong>
            <small>1S SYSTEM ENTERPRISE CONSOLE</small>
          </div>
        </div>
        <div class="enterprise-login-eyebrow">
          <span></span>
          SECURE ACCESS
        </div>
        <h2>{{ $t('authentication.loginPanel.title') }}</h2>
        <p>{{ $t('authentication.loginPanel.subtitle') }}</p>
        <div class="enterprise-login-status">
          <div
            v-for="item in enterpriseStatusItems"
            :key="item.title"
            class="enterprise-login-status-item"
          >
            <span class="enterprise-login-status-icon">
              <IconifyIcon :icon="item.icon" class="size-4" />
            </span>
            <div>
              <strong>{{ item.title }}</strong>
              <small>{{ item.description }}</small>
            </div>
          </div>
        </div>
        <div class="enterprise-login-guard">
          <IconifyIcon class="size-4" icon="lucide:shield-check" />
          <span>{{ $t('authentication.loginPanel.guardTip') }}</span>
        </div>
      </div>
    </template>
  </AuthenticationLogin>
</template>

<style scoped>
.enterprise-login-heading {
  margin-bottom: 28px;
}

.enterprise-login-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 22px;
}

.enterprise-login-brand img {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  box-shadow:
    0 16px 36px hsl(var(--primary) / 18%),
    0 0 0 1px hsl(var(--border));
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

.enterprise-login-brand small {
  overflow: hidden;
  margin-top: 5px;
  color: hsl(var(--muted-foreground));
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.enterprise-login-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
  color: hsl(var(--primary));
  font-size: 12px;
  font-weight: 800;
  line-height: 1;
}

.enterprise-login-eyebrow span {
  display: inline-block;
  width: 28px;
  height: 2px;
  border-radius: 999px;
  background: hsl(var(--primary));
  box-shadow: 0 0 18px hsl(var(--primary) / 42%);
}

.enterprise-login-heading h2 {
  margin: 0 0 13px;
  color: hsl(var(--foreground));
  font-size: 38px;
  font-weight: 900;
  letter-spacing: 0;
  line-height: 1.08;
}

.enterprise-login-heading p {
  margin: 0;
  color: hsl(var(--muted-foreground));
  font-size: 14px;
  line-height: 1.7;
}

.enterprise-login-status {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 22px;
}

.enterprise-login-status-item {
  display: flex;
  min-width: 0;
  align-items: flex-start;
  gap: 9px;
  border: 1px solid hsl(var(--border));
  border-radius: 12px;
  padding: 11px 12px 12px;
  background:
    linear-gradient(180deg, hsl(var(--background) / 78%), hsl(var(--background) / 48%)),
    hsl(var(--background));
  box-shadow:
    0 14px 34px hsl(var(--primary) / 7%),
    inset 0 1px 0 hsl(var(--foreground) / 5%);
}

.enterprise-login-status-icon {
  display: inline-flex;
  width: 26px;
  height: 26px;
  flex: 0 0 26px;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: hsl(var(--primary) / 12%);
  color: hsl(var(--primary));
  box-shadow: 0 0 18px hsl(var(--primary) / 18%);
}

.enterprise-login-status-item strong {
  overflow: hidden;
  color: hsl(var(--foreground));
  font-size: 12px;
  font-weight: 700;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.enterprise-login-status-item small {
  display: block;
  overflow: hidden;
  margin-top: 4px;
  color: hsl(var(--muted-foreground));
  font-size: 11px;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.enterprise-login-guard {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 14px;
  border: 1px solid hsl(var(--primary) / 14%);
  border-radius: 12px;
  padding: 10px 12px;
  background: hsl(var(--primary) / 6%);
  color: hsl(var(--muted-foreground));
  font-size: 12px;
  line-height: 1.45;
}

.enterprise-login-guard svg {
  flex: 0 0 auto;
  color: hsl(var(--primary));
}

@media (max-width: 480px) {
  .enterprise-login-heading h2 {
    font-size: 32px;
  }

  .enterprise-login-status {
    grid-template-columns: 1fr;
  }
}
</style>
