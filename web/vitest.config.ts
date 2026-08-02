import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vitest/config';

export default defineConfig({
  plugins: [vue()],
  test: {
    environment: 'happy-dom',
    exclude: [
      '**/dist/**',
      '**/node_modules/**',
      'playground/__tests__/e2e/**',
    ],
    include: [
      'internal/**/*.{test,spec}.ts',
      'packages/**/*.{test,spec}.ts',
      'playground/src/**/*.{test,spec}.ts',
    ],
  },
});
