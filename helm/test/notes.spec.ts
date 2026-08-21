/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { describe, it, expect } from 'vitest';
import { execa } from 'execa';
import { mkdirSync, mkdtempSync } from 'node:fs';
import { writeFile } from 'node:fs/promises';
import { stringify } from 'yaml';
import { merge } from 'lodash';
import { dirname, join } from 'node:path';
import { tmpdir } from 'node:os';

const renderNotes = async (values: object): Promise<string> => {
  const tmpValuesPath = join(
    tmpdir(),
    mkdtempSync('helm-notes-'),
    'values.yaml',
  );
  mkdirSync(dirname(tmpValuesPath));
  await writeFile(
    tmpValuesPath,
    stringify(
      merge(
        {
          appVersionOverride: 'test-version',
          snowWhite: {
            host: 'localhost',
            httproute: { enabled: true },
          },
        },
        values,
      ),
    ),
  );

  // Client-side dry-run: NOTES.txt is only rendered by 'install'/'upgrade', not 'template'.
  const { stdout } = await execa('helm', [
    'install',
    'test-release',
    'charts/snow-white',
    '--namespace',
    'default',
    '--dry-run',
    '-f',
    tmpValuesPath,
  ]);

  return stdout;
};

describe('NOTES', () => {
  it('does not warn by default', async () => {
    const stdout = await renderNotes({});

    expect(stdout).not.toContain('WARNING');
  });

  it('warns when disableIngestion is true but influxdb2 is still enabled', async () => {
    const stdout = await renderNotes({
      otelCollector: { disableIngestion: true },
    });

    expect(stdout).toContain(
      "WARNING: 'otelCollector.disableIngestion' is 'true', but 'influxdb2.enabled' is still 'true'.",
    );
  });

  it('does not warn when influxdb2 is disabled alongside disableIngestion', async () => {
    const stdout = await renderNotes({
      otelCollector: { disableIngestion: true },
      influxdb2: { enabled: false },
    });

    expect(stdout).not.toContain('WARNING');
  });

  it('does not warn when disableIngestion is false, regardless of influxdb2', async () => {
    const stdout = await renderNotes({
      otelCollector: { disableIngestion: false },
      influxdb2: { enabled: true },
    });

    expect(stdout).not.toContain('WARNING');
  });
});
