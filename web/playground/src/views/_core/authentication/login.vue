<script lang="ts" setup>
import type { VbenFormSchema } from '@vben/common-ui';
import type { BasicOption, Recordable } from '@vben/types';

import { computed, markRaw, useTemplateRef } from 'vue';

import { AuthenticationLogin, SliderCaptcha, z } from '@vben/common-ui';
import { $t } from '@vben/locales';

import { useAuthStore } from '#/store';

defineOptions({ name: 'Login' });

const authStore = useAuthStore();

const USER_OPTIONS: BasicOption[] = [
  {
    label: 'Admin',
    value: 'admin',
  },
];

const formSchema = computed((): VbenFormSchema[] => {
  return [
    {
      component: 'VbenSelect',
      // componentProps(_values, form) {
      //   return {
      //     'onUpdate:modelValue': (value: string) => {
      //       const findItem = USER_OPTIONS.find(
      //         (item) => item.value === value,
      //       );
      //       if (findItem) {
      //         form.setValues({
      //           password: '123456',
      //           username: findItem.label,
      //         });
      //       }
      //     },
      //     options: USER_OPTIONS,
      //     placeholder: $t('authentication.selectAccount'),
      //   };
      // },
      componentProps: {
        options: USER_OPTIONS,
        placeholder: $t('authentication.selectAccount'),
      },
      fieldName: 'selectAccount',
      label: $t('authentication.selectAccount'),
      rules: z
        .string()
        .min(1, { message: $t('authentication.selectAccount') })
        .optional()
        .default('admin'),
    },
    {
      component: 'VbenInput',
      componentProps: {
        placeholder: $t('authentication.usernameTip'),
      },
      dependencies: {
        trigger(values, form) {
          if (values.selectAccount) {
            const findUser = USER_OPTIONS.find(
              (item) => item.value === values.selectAccount,
            );
            if (findUser) {
              form.setValues({
                password: 'admin123',
                username: findUser.value,
              });
            }
          }
        },
        triggerFields: ['selectAccount'],
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
    title: $t('authentication.loginPanel.authTitle'),
  },
  {
    description: $t('authentication.loginPanel.lockDescription'),
    title: $t('authentication.loginPanel.lockTitle'),
  },
  {
    description: $t('authentication.loginPanel.auditDescription'),
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
        <h2>{{ $t('authentication.loginPanel.title') }}</h2>
        <p>{{ $t('authentication.loginPanel.subtitle') }}</p>
        <div class="enterprise-login-status">
          <div
            v-for="item in enterpriseStatusItems"
            :key="item.title"
            class="enterprise-login-status-item"
          >
            <strong>{{ item.title }}</strong>
            <small>{{ item.description }}</small>
          </div>
        </div>
      </div>
    </template>
  </AuthenticationLogin>
</template>

<style scoped>
.enterprise-login-heading {
  margin-bottom: 28px;
}

.enterprise-login-heading h2 {
  margin: 0 0 10px;
  color: hsl(var(--foreground));
  font-size: 36px;
  font-weight: 800;
  letter-spacing: 0;
  line-height: 1.15;
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
  gap: 8px;
  margin-top: 16px;
}

.enterprise-login-status-item {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
  border: 1px solid hsl(var(--border));
  border-radius: 10px;
  padding: 9px 10px;
  background:
    linear-gradient(180deg, hsl(var(--primary) / 8%), transparent),
    hsl(var(--background));
  box-shadow: 0 10px 26px hsl(var(--primary) / 8%);
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
  overflow: hidden;
  color: hsl(var(--muted-foreground));
  font-size: 11px;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 480px) {
  .enterprise-login-status {
    grid-template-columns: 1fr;
  }
}
</style>
