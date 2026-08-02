import { beforeEach, describe, expect, it, vi } from 'vitest';

const messageMocks = vi.hoisted(() => ({
  hide: vi.fn(),
  message: {
    error: vi.fn(),
    loading: vi.fn(),
    success: vi.fn(),
  },
}));

vi.mock('antdv-next', () => ({
  message: messageMocks.message,
}));

import {
  rejectHrUploadFile,
  resolveHrUploadFile,
  runHrUploadWithFeedback,
} from './upload';

describe('HRMS upload shared helpers', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    messageMocks.message.loading.mockReturnValue(messageMocks.hide);
  });

  it('returns the original File when resolving upload file succeeds', () => {
    const file = new File(['ones'], 'employee.csv', { type: 'text/csv' });
    const onError = vi.fn();

    const result = resolveHrUploadFile({ file, onError }, 'Upload failed');

    expect(result).toBe(file);
    expect(messageMocks.message.error).not.toHaveBeenCalled();
    expect(onError).not.toHaveBeenCalled();
  });

  it('rejects non-File upload input with visible feedback and onError callback', () => {
    const onError = vi.fn();

    const result = resolveHrUploadFile(
      { file: 'not-a-file', onError },
      'Upload failed',
    );

    expect(result).toBeUndefined();
    expect(messageMocks.message.error).toHaveBeenCalledWith('Upload failed');
    expect(onError).toHaveBeenCalledTimes(1);
    expect(onError.mock.calls[0]?.[0]).toBeInstanceOf(Error);
    expect(onError.mock.calls[0]?.[0].message).toBe('Upload failed');
  });

  it('rejects upload file explicitly with feedback and returns the generated error', () => {
    const onError = vi.fn();

    const error = rejectHrUploadFile({ onError }, 'CSV only');

    expect(error).toBeInstanceOf(Error);
    expect(error.message).toBe('CSV only');
    expect(messageMocks.message.error).toHaveBeenCalledWith('CSV only');
    expect(onError).toHaveBeenCalledWith(error);
  });

  it('runs uploader with loading, success feedback and onSuccess callback', async () => {
    const onSuccess = vi.fn();
    const uploader = vi.fn().mockResolvedValue({ successCount: 3 });

    const result = await runHrUploadWithFeedback(
      {
        errorMessage: 'Upload failed',
        loadingMessage: 'Uploading',
        onSuccess,
        successMessage: (data) => `Imported ${data.successCount}`,
      },
      uploader,
    );

    expect(result).toEqual({ data: { successCount: 3 }, success: true });
    expect(messageMocks.message.loading).toHaveBeenCalledWith('Uploading', 0);
    expect(messageMocks.message.success).toHaveBeenCalledWith('Imported 3');
    expect(onSuccess).toHaveBeenCalledWith({ successCount: 3 });
    expect(messageMocks.hide).toHaveBeenCalledTimes(1);
  });

  it('normalizes uploader failure with feedback and onError callback', async () => {
    const onError = vi.fn();
    const uploader = vi.fn().mockRejectedValue('boom');

    const result = await runHrUploadWithFeedback(
      {
        errorMessage: 'Upload failed',
        loadingMessage: 'Uploading',
        onError,
        successMessage: 'Upload success',
      },
      uploader,
    );

    expect(result.success).toBe(false);
    expect(result.error).toBeInstanceOf(Error);
    expect(result.error?.message).toBe('Upload failed');
    expect(messageMocks.message.error).toHaveBeenCalledWith('Upload failed');
    expect(onError).toHaveBeenCalledWith(result.error);
    expect(messageMocks.hide).toHaveBeenCalledTimes(1);
  });
});
