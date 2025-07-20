#!/usr/bin/env node

/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { readFileSync, writeFileSync } from 'node:fs';
import { parse, stringify } from 'yaml';

const newVersion = process.argv[2];

if (!newVersion) {
  console.error('Usage: ts-node bump-chart-version.ts <version>');
  console.error('Example: ts-node bump-chart-version.ts 1.2.3');
  process.exit(1);
}

const chartPath = 'charts/snow-white/Chart.yaml';

try {
  const fileContent = readFileSync(chartPath, 'utf8');
  const values = parse(fileContent);

  values.appVersion = newVersion;

  const updatedContent = stringify(values);
  writeFileSync(chartPath, updatedContent, 'utf8');

  console.log(
    `Successfully updated appVersion to '${newVersion}' in ${chartPath}`,
  );
} catch (error) {
  console.error('Error updating values.yaml:', error);
  process.exit(1);
}
