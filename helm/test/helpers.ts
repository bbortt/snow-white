/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { expect } from 'vitest';

export const getPodSpec = (deployment: any): any => {
  const { spec } = deployment;
  expect(spec).toBeDefined();

  const { template } = spec;
  expect(template).toBeDefined();

  const templateSpec = template.spec;
  expect(templateSpec).toBeDefined();

  return templateSpec;
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
