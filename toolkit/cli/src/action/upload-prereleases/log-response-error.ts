/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import chalk from 'chalk';

import type { GetAllApis500Response } from '../../clients/api-index-api';

const parseResponseFromText = async (error: Error & { response: Response }): Promise<GetAllApis500Response | null> =>
  await error.response
    .text()
    .then(text => (text ? (JSON.parse(text) as GetAllApis500Response) : null))
    .catch(() => null);

export const logResponseError = async (error: Error & { response: Response }): Promise<void> => {
  console.debug(chalk.gray(`\t  Status: ${error.response.status}`));

  const body = (await error.response.json().catch(async () => await parseResponseFromText(error))) as GetAllApis500Response | null;

  if (body) {
    console.error(chalk.red(`\t  Details: ${(body as { message: string }).message}`));
  } else if (error.response.status === 409) {
    console.error(chalk.red(`\t  Error: This API specification has already been indexed!`));
  } else {
    console.error(chalk.red(`\t  Error: ${error.response.statusText}`));
  }
};
