/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { file as tmpFile } from 'tmp-promise';
import { execa } from 'execa';
import { writeFile } from 'node:fs/promises';
import { stringify, parseAllDocuments } from 'yaml';

const withDefaultValues = (values: object): object => {
  return {
    ingress: {
      host: 'localhost',
    },
    ...values,
  };
};

export async function renderHelmChart(options: {
  chartPath: string;
  debug?: boolean;
  namespace?: string;
  releaseName?: string;
  values?: object;
}): Promise<any[]> {
  const {
    chartPath,
    debug = false,
    namespace = 'default',
    releaseName = 'test-release',
    values = {},
  } = options;

  const { path: tmpValuesPath, cleanup } = await tmpFile();
  await writeFile(tmpValuesPath, stringify(withDefaultValues(values)));

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
    const json = docs.map((doc) => doc.toJSON()).filter(Boolean);

    if (debug) {
      console.debug('json:', json);
    }

    return json;
  } finally {
    await cleanup();
  }
}
