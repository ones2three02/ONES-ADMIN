import type {
  ApplicationConfig,
  VbenAdminProAppConfigRaw,
} from '@vben/types/global';

const FEISHU_AUTH_ENDPOINT = 'https://open.feishu.cn/open-apis/authen/v1/index';

/**
 * 由 vite-inject-app-config 注入的全局配置
 */
export function useAppConfig(
  env: Record<string, any>,
  isProduction: boolean,
): ApplicationConfig {
  // 生产环境下，直接使用 window._VBEN_ADMIN_PRO_APP_CONF_ 全局变量
  const config = isProduction
    ? window._VBEN_ADMIN_PRO_APP_CONF_
    : (env as VbenAdminProAppConfigRaw);

  const {
    VITE_GLOB_API_URL,
    VITE_GLOB_AUTH_DINGDING_CORP_ID,
    VITE_GLOB_AUTH_DINGDING_CLIENT_ID,
    VITE_GLOB_AUTH_FEISHU_APP_ID,
    VITE_GLOB_AUTH_FEISHU_AUTH_URL,
    VITE_GLOB_AUTH_FEISHU_REDIRECT_URI,
    VITE_GLOB_AUTH_SSO_NAME,
    VITE_GLOB_AUTH_SSO_URL,
  } = config;

  const applicationConfig: ApplicationConfig = {
    apiURL: VITE_GLOB_API_URL,
    auth: {},
  };
  if (
    isConfiguredValue(VITE_GLOB_AUTH_DINGDING_CORP_ID) &&
    isConfiguredValue(VITE_GLOB_AUTH_DINGDING_CLIENT_ID)
  ) {
    applicationConfig.auth.dingding = {
      clientId: VITE_GLOB_AUTH_DINGDING_CLIENT_ID,
      corpId: VITE_GLOB_AUTH_DINGDING_CORP_ID,
    };
  }
  const feishuAuthUrl = resolveFeishuAuthUrl({
    appId: VITE_GLOB_AUTH_FEISHU_APP_ID,
    authUrl: VITE_GLOB_AUTH_FEISHU_AUTH_URL,
    redirectUri: VITE_GLOB_AUTH_FEISHU_REDIRECT_URI,
  });
  if (feishuAuthUrl) {
    applicationConfig.auth.feishu = {
      name: '飞书登录',
      url: feishuAuthUrl,
    };
  }
  if (isConfiguredValue(VITE_GLOB_AUTH_SSO_URL)) {
    applicationConfig.auth.sso = {
      name: isConfiguredValue(VITE_GLOB_AUTH_SSO_NAME)
        ? VITE_GLOB_AUTH_SSO_NAME
        : 'Enterprise SSO',
      url: VITE_GLOB_AUTH_SSO_URL,
    };
  }

  return applicationConfig;
}

function resolveFeishuAuthUrl({
  appId,
  authUrl,
  redirectUri,
}: {
  appId?: string;
  authUrl?: string;
  redirectUri?: string;
}) {
  if (isConfiguredValue(authUrl)) {
    return authUrl;
  }
  if (!isConfiguredValue(appId) || !isConfiguredValue(redirectUri)) {
    return '';
  }

  const url = new URL(FEISHU_AUTH_ENDPOINT);
  url.searchParams.set('app_id', appId);
  url.searchParams.set('redirect_uri', redirectUri);
  return url.toString();
}

function isConfiguredValue(value?: string): value is string {
  if (!value) {
    return false;
  }
  const trimmed = value.trim();
  return (
    trimmed !== '' &&
    !trimmed.startsWith('应用的') &&
    !trimmed.startsWith('飞书应用') &&
    !trimmed.toLowerCase().startsWith('your-')
  );
}
