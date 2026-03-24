/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import axios, { AxiosError } from 'axios';
import chalk from 'chalk';
import { load } from 'js-yaml';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { exit } from 'node:process';

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
  url?: string;
  configFile?: string;
  apiNamePath?: string;
  apiVersionPath?: string;
  serviceNamePath?: string;
}

const resolveUrl = (cliUrl?: string, configFile?: string): string => {
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

export const uploadPrereleases = async (options: UploadPrereleasesOptions): Promise<void> => {
  const { globPattern } = options;
  const url = resolveUrl(options.url, options.configFile);
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
    const absolutePath = resolve(process.cwd(), file);
    try {
      const content = readFileSync(absolutePath, 'utf8');
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

      await axios.post(
        `${url}/api/rest/v1/apis`,
        {
          apiName,
          apiType: 'OPENAPI',
          apiVersion,
          content,
          prerelease: true,
          serviceName,
          sourceUrl: `file://${absolutePath}`,
        },
        { headers: { 'Content-Type': 'application/json' } },
      );

      console.log(chalk.green(`✅  ${file}: Uploaded ${serviceName}/${apiName}@${apiVersion}`));
      successCount++;
    } catch (error: unknown) {
      console.error(chalk.red(`❌  ${file}: Upload failed.`));

      if (error instanceof AxiosError && error.response) {
        console.error(chalk.red(`\t  Status: ${error.response.status}`));
        if (error.response.data && Object.hasOwn(error.response.data as object, 'message')) {
          console.error(chalk.red(`\t  Details: ${(error.response.data as { message: string }).message}`));
        } else {
          console.error(chalk.red(`\t  Error: ${error.response.statusText}`));
        }
      } else if (error instanceof AxiosError && error.request) {
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
