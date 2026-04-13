/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import chalk from 'chalk';
import { load } from 'js-yaml';
import { readFileSync } from 'node:fs';
import { exit } from 'node:process';

import type { ApiIndexApi, GetAllApis200ResponseInner, GetAllApis500Response } from '../clients/api-index-api';
import type { UploadPrereleasesOptions } from '../config/sanitized-options.ts';

import { GetAllApis200ResponseInnerApiTypeEnum } from '../clients/api-index-api';
import { PRERELEASE_UPLOAD_FAILED } from '../common/exit-codes';
import { scanGlob } from '../common/glob';
import { DEFAULT_API_NAME_PATH, DEFAULT_API_VERSION_PATH, DEFAULT_SERVICE_NAME_PATH, extractApiSpecMetadata } from '../common/openapi';

const isResponseError = (error: unknown): error is Error & { response: Response } =>
  error instanceof Error && error.name === 'ResponseError' && 'response' in error;

const isFetchError = (error: unknown): boolean => error instanceof TypeError || (error instanceof Error && error.name === 'FetchError');

const logResponseError = async (error: Error & { response: Response }): Promise<void> => {
  console.debug(chalk.red(`\t  Status: ${error.response.status}`));
  const body = (await error.response.json().catch(() => null)) as GetAllApis500Response | undefined;
  if (body) {
    console.error(chalk.red(`\t  Details: ${(body as { message: string }).message}`));
  } else if (error.response.status === 409) {
    console.error(chalk.red(`\t  Error: This API specification has already been indexed!`));
  } else {
    console.error(chalk.red(`\t  Error: ${error.response.statusText}`));
  }
};

interface UploadErrorResult {
  ignored: boolean;
}

const handleUploadError = async (
  error: unknown,
  file: string,
  metadata: { apiName?: string; apiVersion?: string; serviceName?: string },
  ignoreExisting: boolean,
): Promise<UploadErrorResult> => {
  console.error(chalk.red(`❌  ${file}: Upload failed.`));

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

export const uploadPrereleases = async (apiIndexApi: ApiIndexApi, options: UploadPrereleasesOptions): Promise<void> => {
  const { globPattern, ignoreExisting, url } = options;
  const apiNamePath = options.apiNamePath ?? DEFAULT_API_NAME_PATH;
  const apiVersionPath = options.apiVersionPath ?? DEFAULT_API_VERSION_PATH;
  const serviceNamePath = options.serviceNamePath ?? DEFAULT_SERVICE_NAME_PATH;

  console.log(chalk.blue(`🚀  Uploading prerelease API specifications matching: ${globPattern}`));
  console.log(chalk.gray(`Base URL: ${url}`));
  console.log(chalk.yellow('⚠️  Prerelease uploads are temporary and will be cleaned up asynchronously after the pipeline completes.'));
  console.log('');

  const files = scanGlob(globPattern, process.cwd());

  if (files.length === 0) {
    console.warn(chalk.yellow(`⚠️  No files matched the pattern: ${globPattern}`));
    return;
  }

  console.log(chalk.gray(`Found ${files.length} file(s) to upload.`));
  console.log('');

  let successCount = 0;
  let failCount = 0;
  let ignoredCount = 0;

  for (const file of files) {
    let uploadedMetadata: { apiName?: string; apiVersion?: string; serviceName?: string } = {};

    try {
      const content = readFileSync(file, 'utf8');
      const parsed = load(content);
      const result = extractApiSpecMetadata(parsed, { apiNamePath, apiVersionPath, serviceNamePath });

      if (!result.ok) {
        console.error(chalk.red(`❌  ${file}: Missing required metadata fields.`));
        result.missing.forEach(path => console.error(chalk.red(`\t  '${path}' not found or empty.`)));
        failCount++;
        continue;
      }

      const { apiName, apiVersion, serviceName } = result.metadata;
      uploadedMetadata = { apiName, apiVersion, serviceName };

      const getAllApis200ResponseInner: GetAllApis200ResponseInner = {
        apiName,
        apiType: GetAllApis200ResponseInnerApiTypeEnum.Openapi,
        apiVersion,
        content,
        prerelease: true,
        serviceName,
        sourceUrl: `${url}/api/rest/v1/apis/${serviceName}/${apiName}/${apiVersion}/raw`,
      };

      await apiIndexApi.ingestApi({ getAllApis200ResponseInner });

      console.log(chalk.green(`✅  ${file}: Uploaded ${serviceName}/${apiName}@${apiVersion}`));
      successCount++;
    } catch (error: unknown) {
      const { ignored } = await handleUploadError(error, file, uploadedMetadata, ignoreExisting ?? false);
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
