/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import chalk from 'chalk';

import type { ApiSpecMetadata } from '../../common/openapi.ts';

import { isFetchError, isResponseError } from '../../common/error-response-utils.ts';
import { logResponseError } from './log-response-error.ts';

interface UploadErrorResult {
  ignored: boolean;
}

export const handleUploadError = async (
  error: unknown,
  file: string,
  metadata: ApiSpecMetadata,
  ignoreExisting: boolean,
): Promise<UploadErrorResult> => {
  console.error(chalk.red(`❌ ${file}: Upload failed.`));

  if (isResponseError(error)) {
    if (error.response.status === 409 && ignoreExisting) {
      console.warn(chalk.yellow(`\t  ⚠️ Ignoring already existing ${metadata.serviceName}/${metadata.apiName}@${metadata.apiVersion}`));
      return { ignored: true };
    }
    await logResponseError(error);
  } else if (isFetchError(error)) {
    console.error(chalk.red('\t  No response received from server.'));
    console.error(chalk.gray('\t  Check if the service is running and accessible.'));
  } else {
    console.error(chalk.red(`\t  Error: ${error instanceof Error ? error.message : JSON.stringify(error)}`));
  }

  return { ignored: false };
};
