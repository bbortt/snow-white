#!/usr/bin/env node

/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { readFileSync, writeFileSync } from 'node:fs';
import { parse, stringify } from 'yaml';

export interface FileSystem {
  readFileSync(path: string, encoding: BufferEncoding): string;
  writeFileSync(path: string, data: string, encoding: BufferEncoding): void;
}

export interface Logger {
  log(message: string): void;
  error(message: string): void;
}

export interface ProcessExit {
  exit(code: number): void;
}

export class ChartVersionBumper {
  constructor(
    private fs: FileSystem = {
      readFileSync: (path, encoding) => readFileSync(path, encoding),
      writeFileSync: (path, data, encoding) =>
        writeFileSync(path, data, encoding),
    },
    private logger: Logger = console,
  ) {}

  validateVersion(version: string | undefined): string {
    if (!version || version.trim() === '') {
      throw new Error('Version is required');
    }
    return version.trim();
  }

  bumpChartVersion(
    chartVersion: string,
    chartPath: string = 'charts/snow-white/Chart.yaml',
  ): void {
    const validVersion = this.validateVersion(chartVersion);

    try {
      const fileContent = this.fs.readFileSync(chartPath, 'utf8');
      const values = parse(fileContent);

      if (!values || typeof values !== 'object') {
        throw new Error('Invalid YAML structure in chart file');
      }

      values.version = validVersion;
      values.appVersion = validVersion;

      const updatedContent = stringify(values);
      this.fs.writeFileSync(chartPath, updatedContent, 'utf8');

      this.logger.log(
        `Successfully updated appVersion to '${validVersion}' in ${chartPath}`,
      );
    } catch (error) {
      const errorMessage =
        error instanceof Error ? error.message : String(error);
      this.logger.error(`Error updating chart version: ${errorMessage}`);
      throw error;
    }
  }
}

// CLI runner function
export function runCLI(
  args: string[] = process.argv,
  bumper: ChartVersionBumper = new ChartVersionBumper(),
): void {
  const newVersion = args[2];

  if (!newVersion) {
    console.error(
      'Usage: ts-node bump-chart-version.ts <version> <appVersion?>',
    );
    console.error('Example: ts-node bump-chart-version.ts 1.2.3');
    process.exit(1);
  }

  try {
    bumper.bumpChartVersion(newVersion);
  } catch (error) {
    process.exit(2);
  }
}

// Only run if this file is executed directly
if (require.main === module) {
  runCLI();
}
