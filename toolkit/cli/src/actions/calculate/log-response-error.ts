/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import chalk from 'chalk';

import type { CalculateQualityGate400Response } from '../../clients/quality-gate-api';

const parseResponseFromText = async (error: Error & { response: Response }): Promise<CalculateQualityGate400Response | null> =>
  await error.response
    .text()
    .then(text => (text ? (JSON.parse(text) as CalculateQualityGate400Response) : null))
    .catch(() => null);

export const logResponseError = async (error: Error & { response: Response }): Promise<void> => {
  console.debug(chalk.gray(`\t  Status: ${error.response.status}`));

  const body = (await error.response
    .json()
    .catch(async () => await parseResponseFromText(error))) as CalculateQualityGate400Response | null;

  if (body && Object.hasOwn(body, 'message')) {
    console.error(chalk.red(`\t  Details: ${body.message}`));
  } else {
    console.error(chalk.red(`\t  Error: ${error.response.statusText}`));
  }
};
