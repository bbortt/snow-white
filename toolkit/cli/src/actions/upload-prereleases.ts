/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import chalk from 'chalk';
import { load } from 'js-yaml';
import { readFileSync } from 'node:fs';
import { exit } from 'node:process';

import type {
  ApiIndexApi,
  GetAllApis200ResponseInner,
  GetAllApis500Response,
  GetAllApis200ResponseInnerApiTypeEnum,
} from '../clients/api-index-api';

import { INVALID_CONFIG_FORMAT, PRERELEASE_UPLOAD_FAILED } from '../common/exit-codes';
import { scanGlob } from '../common/glob';
import { resolveConfig } from '../config/resolve-config';

// Default JSON paths mirror the api-sync-job's ArtifactoryProperties defaults.
// The api-sync-job uses a parsed OpenAPI object model where extension fields are
// normalised into an 'extensions' map (e.g. info.extensions.x-service-name).
// The CLI reads raw YAML, so extensions sit directly on their parent object
// (e.g. info.x-service-name).
export const DEFAULT_API_NAME_PATH = 'info.title';
export const DEFAULT_API_VERSION_PATH = 'info.version';
export const DEFAULT_SERVICE_NAME_PATH = 'info.x-service-name';

export interface UploadPrereleasesOptions {
  globPattern: string;
  url: string;
  apiNamePath?: string;
  apiVersionPath?: string;
  serviceNamePath?: string;
}

export const resolveUrl = (cliUrl?: string, configFile?: string): string => {
  if (cliUrl) {
    return cliUrl;
  }

  const { url } = resolveConfig(configFile);
  if (!url) {
    console.error(chalk.red('❌  Snow-White base URL must be defined via --url or in the configuration file.'));
    exit(INVALID_CONFIG_FORMAT);
  }
  return url;
};

const getNestedValue = (obj: unknown, path: string): string | undefined => {
  const parts = path.split('.');
  let current: unknown = obj;
  for (const part of parts) {
    if (current && typeof current === 'object') {
      current = (current as Record<string, unknown>)[part];
    } else {
      return undefined;
    }
  }
  return typeof current === 'string' ? current : undefined;
};

const isResponseError = (error: unknown): error is Error & { response: Response } =>
  error instanceof Error && error.name === 'ResponseError' && 'response' in error;

const isFetchError = (error: unknown): boolean => error instanceof TypeError || (error instanceof Error && error.name === 'FetchError');

export const uploadPrereleases = async (apiIndexApi: ApiIndexApi, options: UploadPrereleasesOptions): Promise<void> => {
  const { globPattern, url } = options;
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

  for (const file of files) {
    try {
      const content = readFileSync(file, 'utf8');
      const parsed = load(content);

      const apiName = getNestedValue(parsed, apiNamePath);
      const apiVersion = getNestedValue(parsed, apiVersionPath);
      const serviceName = getNestedValue(parsed, serviceNamePath);

      if (!apiName || !apiVersion || !serviceName) {
        console.error(chalk.red(`❌  ${file}: Missing required metadata fields.`));
        if (!apiName) {
          console.error(chalk.red(`\t  '${apiNamePath}' not found or empty.`));
        }
        if (!apiVersion) {
          console.error(chalk.red(`\t  '${apiVersionPath}' not found or empty.`));
        }
        if (!serviceName) {
          console.error(chalk.red(`\t  '${serviceNamePath}' not found or empty.`));
        }
        failCount++;
        continue;
      }

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
      console.error(chalk.red(`❌  ${file}: Upload failed.`));

      if (isResponseError(error)) {
        console.error(chalk.red(`\t  Status: ${error.response.status}`));
        const body = (await error.response.json().catch(() => null)) as GetAllApis500Response | undefined;
        if (body) {
          console.error(chalk.red(`\t  Details: ${(body as { message: string }).message}`));
        } else {
          console.error(chalk.red(`\t  Error: ${error.response.statusText}`));
        }
      } else if (isFetchError(error)) {
        console.error(chalk.red('\t  No response received from server.'));
        console.error(chalk.gray('\t  Check if the service is running and accessible.'));
      } else {
        console.error(chalk.red(`\t  Error: ${error instanceof Error ? error.message : JSON.stringify(error)}`));
      }

      failCount++;
    }
  }

  console.log('');
  console.log(chalk.blue(`Upload complete: ${successCount} succeeded, ${failCount} failed.`));

  if (failCount > 0) {
    exit(PRERELEASE_UPLOAD_FAILED);
  }
};
