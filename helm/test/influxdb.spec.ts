/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { describe, it, expect } from 'vitest';
import { renderHelmChart } from './render-helm-chart';

const getInfluxDBStatefulSet = (manifests: any[]) => {
  const statefulSet = manifests.find(
    (m) =>
      m.kind === 'StatefulSet' &&
      m.metadata.name.startsWith('test-release-influxdb2'),
  );
  expect(statefulSet).toBeDefined();
  return statefulSet;
};

describe('InfluxDB', () => {
  it('is enabled by default', async () => {
    const manifests = await renderHelmChart({
      chartPath: 'charts/snow-white',
    });

    const statefulSet = getInfluxDBStatefulSet(manifests);
    expect(statefulSet.spec.template.spec.containers[0].image).toMatch(
      /^(registry-\d\.)?influxdb:.+$/,
    );
  });

  it('should have PodDisruptionBudget disabled by default', async () => {
    const manifests = await renderHelmChart({
      chartPath: 'charts/snow-white',
    });

    const podDisruptionBudget = manifests.find(
      (m) =>
        m.kind === 'PodDisruptionBudget' &&
        m.metadata.name.startsWith('test-release-influxdb2'),
    );
    expect(podDisruptionBudget).toBeUndefined();
  });

  it('is NOT high-available by default', async () => {
    const manifests = await renderHelmChart({
      chartPath: 'charts/snow-white',
    });

    const statefulSet = getInfluxDBStatefulSet(manifests);
    expect(statefulSet.spec.replicas).toBe(1);
  });

  it('can be disabled with values', async () => {
    const manifests = await renderHelmChart({
      chartPath: 'charts/snow-white',
      values: {
        influxdb2: {
          enabled: false,
        },
      },
    });

    const influxdbResources = manifests.find((m) =>
      m.metadata.name.startsWith('test-release-influxdb2'),
    );

    expect(influxdbResources).toBeUndefined();
  });

  describe('authentication', () => {
    const extractSecret = (manifests: Array<any>): any | undefined => {
      return manifests.find(
        (m) =>
          m.kind === 'Secret' &&
          m.metadata.name === 'test-release-influxdb2-auth',
      );
    };

    it('exposes credentials using secret by default', async () => {
      const manifests = await renderHelmChart({
        chartPath: 'charts/snow-white',
      });

      const influxdbSecret = extractSecret(manifests);

      expect(influxdbSecret).toBeDefined();
      expect(influxdbSecret.data['admin-token']).toBeDefined();
    });

    it('does not expose secret if defined by user', async () => {
      const manifests = await renderHelmChart({
        chartPath: 'charts/snow-white',
        values: {
          influxdb2: {
            adminUser: {
              existingSecret: 'influxdb-auth',
            },
          },
        },
      });

      const influxdbSecret = extractSecret(manifests);

      expect(influxdbSecret).toBeUndefined();
    });
  });
});
