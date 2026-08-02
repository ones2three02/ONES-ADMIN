import { beforeEach, describe, expect, it, vi } from 'vitest';

import { loadScript } from '../resources';

describe('loadScript', () => {
  beforeEach(() => {
    document.head.innerHTML = '';
    vi.restoreAllMocks();
  });

  it('should resolve when the script loads successfully', async () => {
    let capturedScript: HTMLScriptElement | null = null;
    const appendSpy = vi
      .spyOn(document.head, 'append')
      .mockImplementation((...nodes) => {
        for (const node of nodes) {
          if (node instanceof HTMLScriptElement) {
            capturedScript = node;
          }
        }
      });

    const promise = loadScript('/test-script.js');

    expect(capturedScript).toBeTruthy();
    if (!capturedScript) {
      throw new Error('Expected the captured script element to exist');
    }
    capturedScript.dispatchEvent(new Event('load'));

    await expect(promise).resolves.toBeUndefined();
    appendSpy.mockRestore();
  });

  it('should not insert duplicate script and resolve immediately if already loaded', async () => {
    const existing = document.createElement('script');
    existing.setAttribute('src', 'bar.js');
    const appendSpy = vi.spyOn(document.head, 'append');
    const querySelectorSpy = vi
      .spyOn(document, 'querySelector')
      .mockReturnValue(existing);

    const promise = loadScript('bar.js');

    await expect(promise).resolves.toBeUndefined();
    expect(querySelectorSpy).toHaveBeenCalledWith('script[src="bar.js"]');
    expect(appendSpy).not.toHaveBeenCalled();
  });

  it('should reject when the script fails to load', async () => {
    let capturedScript: HTMLScriptElement | null = null;

    // 拦截 append，捕获 script 元素但不插入 DOM，
    // 防止 happy-dom v20+ 自动触发 load 事件
    const appendSpy = vi
      .spyOn(document.head, 'append')
      .mockImplementation((...nodes) => {
        for (const node of nodes) {
          if (node instanceof HTMLScriptElement) {
            capturedScript = node;
          }
        }
      });

    const promise = loadScript('error.js');

    expect(capturedScript).toBeTruthy();
    if (!capturedScript) {
      throw new Error('Expected the captured script element to exist');
    }
    capturedScript.dispatchEvent(new Event('error'));

    await expect(promise).rejects.toThrow('Failed to load script: error.js');
    appendSpy.mockRestore();
  });

  it('should handle multiple concurrent calls and only insert one script tag', async () => {
    let capturedScript: HTMLScriptElement | null = null;
    let existingScript: HTMLScriptElement | null = null;
    const querySelectorSpy = vi
      .spyOn(document, 'querySelector')
      .mockImplementation(() => existingScript);
    const appendSpy = vi
      .spyOn(document.head, 'append')
      .mockImplementation((...nodes) => {
        for (const node of nodes) {
          if (node instanceof HTMLScriptElement) {
            capturedScript = node;
            existingScript = node;
          }
        }
      });

    const p1 = loadScript('/test-script.js');
    const p2 = loadScript('/test-script.js');

    expect(capturedScript).toBeTruthy();
    if (!capturedScript) {
      throw new Error('Expected the captured script element to exist');
    }
    capturedScript.dispatchEvent(new Event('load'));

    await expect(p1).resolves.toBeUndefined();
    await expect(p2).resolves.toBeUndefined();

    expect(querySelectorSpy).toHaveBeenCalledTimes(2);
    expect(appendSpy).toHaveBeenCalledTimes(1);
    appendSpy.mockRestore();
  });
});
