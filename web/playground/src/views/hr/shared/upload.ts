import { message } from 'antdv-next';

import { errorMessageOf, normalizeError } from './error';

export async function runHrUploadWithFeedback<T>(
  options: {
    errorMessage: string;
    loadingMessage: string;
    onError?: (error: Error) => void;
    onSuccess?: (data: T) => void;
    successMessage: string | ((data: T) => string);
  },
  uploader: () => Promise<T>,
) {
  const hide = message.loading(options.loadingMessage, 0);
  try {
    const data = await uploader();
    const successMessage =
      typeof options.successMessage === 'function'
        ? options.successMessage(data)
        : options.successMessage;
    message.success(successMessage);
    options.onSuccess?.(data);
    return { data, success: true as const };
  } catch (error) {
    const normalizedError = normalizeError(error, options.errorMessage);
    message.error(errorMessageOf(normalizedError, options.errorMessage));
    options.onError?.(normalizedError);
    return { error: normalizedError, success: false as const };
  } finally {
    hide();
  }
}
