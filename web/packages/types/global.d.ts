import type { RouteMeta as IRouteMeta } from '@vben-core/typings';

import 'vue-router';

declare module 'vue-router' {
  // oxlint-disable-next-line typescript/no-empty-object-type
  interface RouteMeta extends IRouteMeta {}
}

export interface VbenAdminProAppConfigRaw {
  VITE_GLOB_API_URL: string;
  VITE_GLOB_AUTH_DINGDING_CLIENT_ID: string;
  VITE_GLOB_AUTH_DINGDING_CORP_ID: string;
  VITE_GLOB_AUTH_FEISHU_APP_ID?: string;
  VITE_GLOB_AUTH_FEISHU_AUTH_URL?: string;
  VITE_GLOB_AUTH_FEISHU_REDIRECT_URI?: string;
  VITE_GLOB_AUTH_SSO_NAME?: string;
  VITE_GLOB_AUTH_SSO_URL?: string;
}

interface AuthConfig {
  dingding?: {
    clientId: string;
    corpId: string;
  };
  feishu?: {
    name: string;
    url: string;
  };
  sso?: {
    name: string;
    url: string;
  };
}

export interface ApplicationConfig {
  apiURL: string;
  auth: AuthConfig;
}

declare global {
  interface Window {
    _VBEN_ADMIN_PRO_APP_CONF_: VbenAdminProAppConfigRaw;
  }
}
