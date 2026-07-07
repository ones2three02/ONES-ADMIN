import { Modal } from 'antdv-next';

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
