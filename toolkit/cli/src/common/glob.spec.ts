/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { afterEach, describe, expect, test } from 'bun:test';
import { mkdtempSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

import { scanGlob } from './glob';

describe('scanGlob', () => {
  const tempDir = mkdtempSync(join(tmpdir(), 'scan-glob-'));

  afterEach(() => {
    rmSync(tempDir, { force: true, recursive: true });
  });

  test('returns matches in deterministic lexicographical order', () => {
    writeFileSync(join(tempDir, 'b.txt'), '');
    writeFileSync(join(tempDir, 'a.txt'), '');
    writeFileSync(join(tempDir, 'c.txt'), '');

    expect(scanGlob('*.txt', tempDir)).toEqual(['a.txt', 'b.txt', 'c.txt']);
  });
});
