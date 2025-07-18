/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { describe, it, expect } from 'vitest';
import { renderHelmChart } from './render-helm-chart';

const getInfluxDBDeployment = (manifests: any[]) => {
  const deployment = manifests.find(
    (m) =>
      m.kind === 'Deployment' &&
      m.metadata.name.startsWith('test-release-influxdb'),
  );
  expect(deployment).toBeDefined();
  return deployment;
};

describe('InfluxDB', () => {
  it('is enabled by default', async () => {
    const manifests = await renderHelmChart({
      chartPath: 'charts/snow-white',
    });

    const deployment = getInfluxDBDeployment(manifests);
    expect(deployment.spec.template.spec.containers[0].image).toMatch(
      /^docker.io\/bitnami\/influxdb:.*$/,
    );
  });

  it('is NOT high-available default', async () => {
    const manifests = await renderHelmChart({
      chartPath: 'charts/snow-white',
      debug: true,
    });

    const statefulSet = getInfluxDBDeployment(manifests);
    expect(statefulSet.spec.replicas).toBeUndefined();
  });
});
