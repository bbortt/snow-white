/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { describe, it, expect } from 'vitest';
import { renderHelmChart } from './render-helm-chart';

const getRedisMaster = (manifests: any[]) => {
  const statefulSet = manifests.find(
    (m) =>
      m.kind === 'StatefulSet' &&
      m.metadata.name === 'test-release-redis-master',
  );
  expect(statefulSet).toBeDefined();
  return statefulSet;
};

const getRedisReplicas = (manifests: any[]) => {
  const statefulSet = manifests.find(
    (m) =>
      m.kind === 'StatefulSet' &&
      m.metadata.name === 'test-release-redis-replicas',
  );
  expect(statefulSet).toBeDefined();
  return statefulSet;
};

describe('Redis', () => {
  it('should be enabled master by default', async () => {
    const manifests = await renderHelmChart({
      chartPath: 'charts/snow-white',
    });

    const redisMaster = getRedisMaster(manifests);
    expect(redisMaster.spec.template.spec.containers[0].image).toMatch(
      /^(registry-\d\.)?docker\.io\/bitnami\/redis:.+$/,
    );
  });

  it('should be enabled replicas by default', async () => {
    const manifests = await renderHelmChart({
      chartPath: 'charts/snow-white',
    });

    const redisReplicas = getRedisReplicas(manifests);
    expect(redisReplicas.spec.template.spec.containers[0].image).toMatch(
      /^(registry-\d\.)?docker\.io\/bitnami\/redis:.+$/,
    );
  });

  it('is high-available by default', async () => {
    const manifests = await renderHelmChart({
      chartPath: 'charts/snow-white',
    });

    const redisReplicas = getRedisReplicas(manifests);
    expect(redisReplicas.spec.replicas).toBe(3);
  });

  it('can be disabled via properties', async () => {
    const manifests = await renderHelmChart({
      chartPath: 'charts/snow-white',
      values: {
        redis: {
          enabled: false,
        },
      },
    });

    const redisResources = manifests.find((m) =>
      m.metadata.name.startsWith('test-release-redis'),
    );

    expect(redisResources).toBeUndefined();
  });
});
