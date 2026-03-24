/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

export const scanGlob = (pattern: string, cwd: string): string[] => {
  const glob = new Bun.Glob(pattern);
  return [...glob.scanSync({ cwd })].sort();
};
