/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { defineConfig } from 'vitest/config';

const reporters = ['default', 'junit'];
if (process.env.CI) {
  reporters.push('github-actions');
}

export default defineConfig({
  test: {
    environment: 'node',
    globals: true,
    mockReset: true,
    outputFile: 'target/vitest/JUnit-report.xml',
    reporters,
    maxWorkers: 1,
  },
});
