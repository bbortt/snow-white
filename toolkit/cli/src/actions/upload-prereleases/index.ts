/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import chalk from 'chalk';
import { load } from 'js-yaml';
import { readFileSync } from 'node:fs';
import { exit } from 'node:process';

import type { ApiIndexApi } from '../../clients/api-index-api';
import type { ApiSpecMetadata } from '../../common/openapi';
import type { UploadPrereleasesOptions } from '../../config/sanitized-options.ts';

import { PRERELEASE_UPLOAD_FAILED } from '../../common/exit-codes';
import { scanGlob } from '../../common/glob';
import { DEFAULT_API_NAME_PATH, DEFAULT_API_VERSION_PATH, DEFAULT_SERVICE_NAME_PATH, extractApiSpecMetadata } from '../../common/openapi';
import { handleUploadError } from './handle-upload-error.ts';
import { uploadApiSpec } from './upload-api-spec.ts';

export const uploadPrereleases = async (apiIndexApi: ApiIndexApi, options: UploadPrereleasesOptions): Promise<void> => {
  const { globPattern, ignoreExisting, url } = options;
  const apiNamePath = options.apiNamePath ?? DEFAULT_API_NAME_PATH;
  const apiVersionPath = options.apiVersionPath ?? DEFAULT_API_VERSION_PATH;
  const serviceNamePath = options.serviceNamePath ?? DEFAULT_SERVICE_NAME_PATH;

  console.log(chalk.blue(`🚀 Uploading prerelease API specifications matching: ${globPattern}`));
  console.log(chalk.gray(`Base URL: ${url}`));
  console.log(chalk.yellow('⚠️ Prerelease uploads are temporary and will be cleaned up asynchronously after the pipeline completes.'));
  console.log('');

  const files = scanGlob(globPattern, process.cwd());

  if (files.length === 0) {
    console.warn(chalk.yellow(`⚠️ No files matched the pattern: ${globPattern}`));
    return;
  }

  console.log(chalk.gray(`Found ${files.length} file(s) to upload.`));
  console.log('');

  let successCount = 0;
  let failCount = 0;
  let ignoredCount = 0;

  for (const file of files) {
    let uploadedMetadata: ApiSpecMetadata | null = null;

    try {
      const content = readFileSync(file, 'utf8');
      const parsed = load(content);
      const apiSpecMetadata = extractApiSpecMetadata(parsed, { apiNamePath, apiVersionPath, serviceNamePath });

      if (!apiSpecMetadata.ok) {
        console.error(chalk.red(`❌ ${file}: Missing required metadata fields.`));
        apiSpecMetadata.missing.forEach(path => console.error(chalk.red(`\t  '${path}' not found or empty.`)));
        failCount++;
        continue;
      }

      uploadedMetadata = apiSpecMetadata.metadata;
      await uploadApiSpec(apiSpecMetadata.metadata, content, url, apiIndexApi, file);

      console.log('uploadedMetadata after:', uploadedMetadata);

      successCount++;
    } catch (error: unknown) {
      // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
      const { ignored } = await handleUploadError(error, file, uploadedMetadata!, ignoreExisting ?? false);
      if (ignored) {
        ignoredCount++;
      } else {
        failCount++;
      }
    }
  }

  console.log('');
  console.log(chalk.blue(`Upload complete: ${successCount} succeeded, ${failCount + ignoredCount} failed.`));

  if (failCount > 0) {
    exit(PRERELEASE_UPLOAD_FAILED);
  }
};
