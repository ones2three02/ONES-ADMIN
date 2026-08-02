<script lang="ts" setup>
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { $t } from '@vben/locales';

import { Button, Result, Spin } from 'antdv-next';

import { useAuthStore } from '#/store';

defineOptions({ name: 'FeishuOAuthCallback' });

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const failed = ref(false);

onMounted(async () => {
  const code = typeof route.query.code === 'string' ? route.query.code : '';
  const state = typeof route.query.state === 'string' ? route.query.state : '';
  if (!code || !state) {
    failed.value = true;
    return;
  }
  try {
    await authStore.authLoginByOAuth('feishu', code, state);
  } catch {
    failed.value = true;
  }
});

function returnToLogin() {
  router.replace('/auth/login');
}
</script>

<template>
  <Result
    v-if="failed"
    status="error"
    :sub-title="$t('authentication.loginPanel.oauthCallbackFailedTip')"
    :title="$t('authentication.loginPanel.oauthCallbackFailedTitle')"
  >
    <template #extra>
      <Button type="primary" @click="returnToLogin">
        {{ $t('authentication.loginPanel.oauthCallbackReturn') }}
      </Button>
    </template>
  </Result>
  <div v-else class="flex min-h-48 items-center justify-center">
    <Spin :tip="$t('authentication.loginPanel.oauthCallbackProcessing')" />
  </div>
</template>
