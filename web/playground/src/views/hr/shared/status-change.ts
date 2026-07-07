import { message, Modal } from 'antdv-next';

import { errorMessageOf } from './error';

const STATUS_CHANGE_CANCELLED = 'HR_STATUS_CHANGE_CANCELLED';

export function confirmHrStatusChange(content: string, title: string) {
  return new Promise<void>((resolve, reject) => {
    Modal.confirm({
      content,
      onCancel() {
        reject(new Error(STATUS_CHANGE_CANCELLED));
      },
      onOk() {
        resolve();
      },
      title,
    });
  });
}

export function isHrStatusChangeCancelled(error: unknown) {
  return (
    error instanceof Error && error.message === STATUS_CHANGE_CANCELLED
  );
}

export async function runHrStatusChangeWithFeedback(options: {
  confirmContent: string;
  confirmTitle: string;
  errorMessage: string;
  successMessage: string;
  updater: () => Promise<unknown>;
}) {
  try {
    await confirmHrStatusChange(options.confirmContent, options.confirmTitle);
    await options.updater();
    message.success(options.successMessage);
    return true;
  } catch (error) {
    if (isHrStatusChangeCancelled(error)) {
      return false;
    }
    message.error(errorMessageOf(error, options.errorMessage));
    return false;
  }
}
