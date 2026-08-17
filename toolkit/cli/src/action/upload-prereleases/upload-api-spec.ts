/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import chalk from 'chalk';

import type { ApiIndexApi, GetAllApis200ResponseInner } from '../../clients/api-index-api';
import type { ApiSpecMetadata } from '../../common/openapi';

import { GetAllApis200ResponseInnerApiTypeEnum } from '../../clients/api-index-api';

export const uploadApiSpec = async (
  apiSpecMetadata: ApiSpecMetadata,
  content: string,
  url: string,
  apiIndexApi: ApiIndexApi,
  file: string,
): Promise<void> => {
  const { apiName, apiVersion, serviceName } = apiSpecMetadata;

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
};
