import { downloadFileFromBlob } from '@vben/utils';

import { openSystemFile } from '#/api/system/file';

export function formatFileSize(size?: number) {
  if (size === undefined || size === null) {
    return '-';
  }
  if (size < 1024) {
    return `${size} B`;
  }
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`;
  }
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
}

export function downloadHrBlob(fileName: string, source: Blob) {
  downloadFileFromBlob({
    fileName,
    source,
  });
}

export async function openHrFile(
  metadata: Parameters<typeof openSystemFile>[0] | undefined,
) {
  if (!metadata?.storedName) {
    return false;
  }
  await openSystemFile(metadata);
  return true;
}
