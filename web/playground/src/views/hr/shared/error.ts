export function errorMessageOf(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback;
}

export function normalizeError(error: unknown, fallback: string) {
  return error instanceof Error ? error : new Error(fallback);
}
