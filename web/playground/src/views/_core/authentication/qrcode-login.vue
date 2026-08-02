<script lang="ts" setup>
import { AuthenticationQrCodeLogin } from '@vben/common-ui';
import { LOGIN_PATH } from '@vben/constants';
import { $t } from '@vben/locales';

defineOptions({ name: 'QrCodeLogin' });

const qrcodeText = `${globalThis.location?.origin ?? ''}${LOGIN_PATH}?channel=qrcode`;
</script>

<template>
  <AuthenticationQrCodeLogin
    :description="$t('authentication.loginPanel.qrcodeDescription')"
    :login-path="LOGIN_PATH"
    :qrcode-text="qrcodeText"
    :sub-title="$t('authentication.loginPanel.qrcodeSubtitle')"
    :title="$t('authentication.loginPanel.qrcodeTitle')"
  />
</template>
