/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import chalk from 'chalk';
import { writeFileSync } from 'node:fs';

/**
 * Writes one report artifact to disk. The log is what `--agentic` suppresses, never the file:
 * a pipeline that asked for the artifact gets it either way.
 */
export const persistReportArtifact = (outputPath: string, body: string, label: string, agentic: boolean): void => {
  writeFileSync(outputPath, body, 'utf8');

  if (!agentic) {
    console.log(chalk.green(`📄 ${label} written to: ${outputPath}`));
  }
};
