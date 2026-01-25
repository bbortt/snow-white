/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { expect } from 'vitest';

export const expectFailsWithMessageContaining = async (
  callback: Function,
  part: string,
): Promise<void> => {
  let errorMessage: string | undefined;
  try {
    await callback();
  } catch (error) {
    errorMessage = error.message;
    expect(error.message).contains(part);

    return;
  }

  console.error('Error message didn match:', errorMessage);

  expect.fail(`Expected code to throw exception containing message '${part}'!`);
};

export const isSubset = (
  subset: Record<string, string>,
  superset: Record<string, any>,
): boolean => {
  for (const key in subset) {
    if (!(key in superset) || subset[key] !== superset[key]) {
      return false;
    }
  }

  return true;
};

export const expectToHaveDefaultLabelsForMicroservice = (
  labels: { [key: string]: string },
  microservice: string,
) => {
  expect(labels).toEqual({
    'app.kubernetes.io/managed-by': 'Helm',
    'app.kubernetes.io/version': 'test-version',
    'helm.sh/chart': 'snow-white',
    'app.kubernetes.io/component': microservice,
    'app.kubernetes.io/instance': 'test-release',
    'app.kubernetes.io/name': microservice,
    'app.kubernetes.io/part-of': 'snow-white',
  });
};
