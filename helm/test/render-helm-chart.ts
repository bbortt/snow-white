/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { file as tmpFile } from 'tmp-promise';
import { execa } from 'execa';
import { writeFile } from 'node:fs/promises';
import { stringify, parseAllDocuments } from 'yaml';
import { merge } from 'lodash';

const withDefaultValues = (values: object): object => {
  return {
    appVersionOverride: 'test-version',
    ...merge(
      {
        snowWhite: {
          ingress: {
            host: 'localhost',
          },
        },
      },
      values,
    ),
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
    debug = process.env.DEBUG?.toLowerCase() === 'true',
    namespace = 'default',
    releaseName = 'test-release',
    values = {},
  } = options;

  const { path: tmpValuesPath, cleanup } = await tmpFile();
  await writeFile(tmpValuesPath, stringify(withDefaultValues(values)));

  const helmArgs = [
    'template',
    releaseName,
    chartPath,
    '--namespace',
    namespace,
    '-f',
    tmpValuesPath,
  ];

  if (debug) {
    helmArgs.push('--debug');
  }

  try {
    const { stdout } = await execa('helm', helmArgs);

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
