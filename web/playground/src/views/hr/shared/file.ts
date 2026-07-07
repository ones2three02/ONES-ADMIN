import { downloadFileFromBlob } from '@vben/utils';

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
