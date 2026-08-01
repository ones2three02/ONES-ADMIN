import {
  appCopyrightPreferences,
  defineOverridesPreferences,
  preferences,
  updatePreferences,
} from '@vben/preferences';

export interface PlaygroundPreferencesExtension {
  defaultVisibleRows: number;
  enableQuickActions: boolean;
  highlightTone: 'default' | 'success' | 'warning';
  reportTitle: string;
}

const ONES_BRAND_LOGO_LIGHT = '/brand/ones-1s-app-icon-light.png';
const ONES_BRAND_LOGO_DARK = '/brand/ones-1s-app-icon-dark.png';
const LEGACY_ONES_BRAND_LOGO = '/brand/ones-1s-app-icon.png';
const LEGACY_VBEN_LOGO =
  'https://unpkg.com/@vbenjs/static-source@0.1.7/source/logo-v1.webp';

/**
 * @description 项目配置文件
 * 只需要覆盖项目中的一部分配置，不需要的配置不用覆盖，会自动使用默认配置
 * !!! 更改配置后请清空缓存，否则可能不生效
 */
export const overridesPreferences = defineOverridesPreferences({
  // overrides
  app: {
    name: import.meta.env.VITE_APP_TITLE,
  },
  copyright: appCopyrightPreferences,
  logo: {
    fit: 'contain',
    source: ONES_BRAND_LOGO_LIGHT,
    sourceDark: ONES_BRAND_LOGO_DARK,
  },
});

export function migrateBrandPreferences() {
  const shouldReplaceSource =
    preferences.logo.source === LEGACY_VBEN_LOGO ||
    preferences.logo.source === LEGACY_ONES_BRAND_LOGO;
  const shouldReplaceDarkSource =
    !preferences.logo.sourceDark ||
    preferences.logo.sourceDark === LEGACY_VBEN_LOGO ||
    preferences.logo.sourceDark === LEGACY_ONES_BRAND_LOGO;
  const shouldReplaceCopyright =
    !preferences.copyright.icp ||
    preferences.copyright.icp.includes('19024351') ||
    (preferences.copyright.companyName && preferences.copyright.companyName.includes('Vben'));

  if (!shouldReplaceSource && !shouldReplaceDarkSource && !shouldReplaceCopyright) {
    return;
  }

  updatePreferences({
    logo: {
      fit: 'contain',
      ...(shouldReplaceSource ? { source: ONES_BRAND_LOGO_LIGHT } : {}),
      ...(shouldReplaceDarkSource ? { sourceDark: ONES_BRAND_LOGO_DARK } : {}),
    },
    ...(shouldReplaceCopyright ? { copyright: appCopyrightPreferences } : {}),
  });
}
