/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { file as tmpFile } from 'tmp-promise';
import { execa } from 'execa';
import { writeFile } from 'node:fs/promises';
import { stringify, parseAllDocuments } from 'yaml';

export async function renderHelmChart(options: {
  chartPath: string;
  values?: Record<string, any>;
  releaseName?: string;
  namespace?: string;
}): Promise<any[]> {
  const {
    chartPath,
    values = {},
    releaseName = 'test-release',
    namespace = 'default',
  } = options;

  const { path: tmpValuesPath, cleanup } = await tmpFile();
  await writeFile(tmpValuesPath, stringify(values));

  try {
    const { stdout } = await execa('helm', [
      'template',
      releaseName,
      chartPath,
      '--namespace',
      namespace,
      '-f',
      tmpValuesPath,
    ]);

    const docs = parseAllDocuments(stdout);
    return docs.map((doc) => doc.toJSON()).filter(Boolean);
  } finally {
    await cleanup();
  }
}
