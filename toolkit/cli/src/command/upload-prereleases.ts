/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { Command } from 'commander';

import type { CliOptions } from '../config/cli-options';

import { uploadPrereleases as uploadPrereleasesAction } from '../action/upload-prereleases';
import { getApiIndexApi } from '../api/api-index-api';
import { sanitizeUploadPrereleasesOptions } from '../config/sanitize-upload-prereleases-options';

export const uploadPrereleases = (program: Command): void => {
  program
    .command('upload-prereleases')
    .description(
      'Upload one or more API specifications from the local file system as prereleases.\n' +
        'Intended to be called at the start of a pipeline before QA runs.\n' +
        'Uploaded prereleases are temporary and will be cleaned up asynchronously.',
    )
    .option('--api-specs <pattern>', 'Glob pattern selecting which specification files to upload (e.g. "services/**/openapi.yaml")')
    .option('--url <baseUrl>', 'Base URL for Snow-White (overrides config file)')
    .option('--config-file <path>', 'Path to config file (used to resolve --url if not provided directly)')
    .option('--api-name-path <jsonPath>', 'JSON path to the API name field in the specification')
    .option('--api-version-path <jsonPath>', 'JSON path to the API version field in the specification')
    .option(
      '--service-name-path <jsonPath>',
      'JSON path to the service name field in the specification (maps to the x-service-name extension in raw YAML)',
    )
    .option('--ignore-existing', 'Ignore previously indexed API specifications', false)
    .action(async (options: CliOptions) => {
      const sanitizedOptions = sanitizeUploadPrereleasesOptions(options);
      const apiIndexApi = getApiIndexApi(sanitizedOptions.url);
      await uploadPrereleasesAction(apiIndexApi, sanitizedOptions);
    });
};
