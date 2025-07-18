/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { describe, it, expect } from 'vitest';
import { renderHelmChart } from './render-helm-chart';

const getPrimaryStatefulSet = (manifests: any[]) => {
  const statefulSet = manifests.find(
    (m) =>
      m.kind === 'StatefulSet' &&
      m.metadata.name.startsWith('test-release-postgresql-primary'),
  );
  expect(statefulSet).toBeDefined();
  return statefulSet;
};

const getReadReplicas = (manifests: any[]) => {
  const statefulSet = manifests.find(
    (m) =>
      m.kind === 'StatefulSet' &&
      m.metadata.name === 'test-release-postgresql-read',
  );
  expect(statefulSet).toBeDefined();
  return statefulSet;
};

describe('PostgreSQL', () => {
  it('has enabled primary by default', async () => {
    const manifests = await renderHelmChart({
      chartPath: 'charts/snow-white',
    });

    const primaryStatefulSet = getPrimaryStatefulSet(manifests);
    expect(primaryStatefulSet.spec.template.spec.containers[0].image).toMatch(
      /^docker.io\/bitnami\/postgresql:.*$/,
    );
  });

  it('has enabled read-replica by default', async () => {
    const manifests = await renderHelmChart({
      chartPath: 'charts/snow-white',
    });

    const readReplicas = getReadReplicas(manifests);
    expect(readReplicas.spec.template.spec.containers[0].image).toMatch(
      /^docker.io\/bitnami\/postgresql:.*$/,
    );
    expect(readReplicas.spec.replicas).toBe(1);
  });
});
