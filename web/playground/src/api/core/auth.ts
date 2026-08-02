import { baseRequestClient, requestClient } from '#/api/request';

export namespace AuthApi {
  /** 登录接口参数 */
  export interface LoginParams {
    password?: string;
    username?: string;
  }

  /** 登录接口返回值 */
  export interface LoginResult {
    accessToken: string;
  }

  export interface OnesLoginResult {
    permissions: string[];
    roles: string[];
    token: {
      tokenName: string;
      tokenPrefix: string;
      tokenValue: string;
    };
    user: {
      avatar: string;
      displayName: string;
      id: number;
      username: string;
    };
  }

  export interface AuthProviderCapability {
    displayName: string;
    enabled: boolean;
    id: string;
  }

  export interface OAuthAuthorizeResult {
    authorizationUrl: string;
    expiresInSeconds: number;
  }

  export interface OAuthCallbackResult {
    expiresInSeconds: number;
    ticket: string;
  }

}

function normalizeLoginResult(result: AuthApi.OnesLoginResult) {
  return {
    accessToken: result.token.tokenValue,
  };
}

/**
 * 登录
 */
export async function loginApi(data: AuthApi.LoginParams) {
  const result = await requestClient.post<AuthApi.OnesLoginResult>(
    '/auth/login',
    data,
    {
      withCredentials: true,
    },
  );
  return normalizeLoginResult(result);
}

export async function getAuthProvidersApi() {
  return requestClient.get<AuthApi.AuthProviderCapability[]>('/auth/providers');
}

export async function getOAuthAuthorizeApi(provider: string) {
  return requestClient.get<AuthApi.OAuthAuthorizeResult>(
    `/auth/oauth/${provider}/authorize`,
  );
}

export async function completeOAuthCallbackApi(
  provider: string,
  code: string,
  state: string,
) {
  return requestClient.get<AuthApi.OAuthCallbackResult>(
    `/auth/oauth/${provider}/callback`,
    { params: { code, state } },
  );
}

export async function exchangeOAuthTicketApi(ticket: string) {
  const result = await requestClient.post<AuthApi.OnesLoginResult>(
    '/auth/oauth/exchange',
    { ticket },
  );
  return normalizeLoginResult(result);
}

/**
 * 刷新accessToken
 */
export async function refreshTokenApi(token?: null | string) {
  const result = (await baseRequestClient.post('/auth/refresh', null, {
    headers: token ? { Authorization: `Bearer ${token}` } : undefined,
    withCredentials: true,
  })) as { data: { data: string } };
  return result.data.data;
}

/**
 * 退出登录
 */
export async function logoutApi() {
  return baseRequestClient.post('/auth/logout', null, {
    withCredentials: true,
  });
}

/**
 * 获取用户权限码
 */
export async function getAccessCodesApi() {
  return requestClient.get<string[]>('/auth/codes');
}
