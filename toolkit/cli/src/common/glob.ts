/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

/**
 * !! Visible for testing !!
 *
 * Extracted so it can be unit-tested by direct invocation - `bun test --coverage` does not
 * reliably credit branch hits inside a callback invoked only through the native
 * `Array.prototype.sort()`.
 */
export const compareLexicographically = (a: string, b: string): number => {
  if (a < b) {
    return -1;
  }

  if (a > b) {
    return 1;
  }

  return 0;
};

export const scanGlob = (pattern: string, cwd: string): string[] => {
  const glob = new Bun.Glob(pattern);
  return [...glob.scanSync({ cwd })].sort(compareLexicographically);
};
