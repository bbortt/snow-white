/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { describe, it, expect } from 'vitest';
import { renderHelmChart } from './render-helm-chart';

const getKafkaStatefulSet = async (manifests?: any[]) => {
  if (!manifests) {
    manifests = await renderHelmChart({
      chartPath: 'charts/snow-white',
    });
  }

  const statefulSet = manifests.find(
    (m) =>
      m.kind === 'StatefulSet' &&
      m.metadata.name === 'snow-white-kafka-test-release',
  );
  expect(statefulSet).toBeDefined();
  return statefulSet;
};

describe('Kafka', () => {
  it('can be disabled via properties', async () => {
    const manifests = await renderHelmChart({
      chartPath: 'charts/snow-white',
      values: {
        kafka: {
          enabled: false,
        },
      },
    });

    const kafkaResources = manifests.find((m) =>
      m.metadata.name.startsWith('test-release-kafka'),
    );

    expect(kafkaResources).toBeUndefined();
  });

  describe('StatefulSet', () => {
    it('is enabled by default', async () => {
      const statefulSet = await getKafkaStatefulSet();

      expect(statefulSet.spec.template.spec.containers[0].image).toMatch(
        /^(registry-\d\.)?docker\.io\/confluentinc\/cp-kafka:.+$/,
      );
    });

    it('is high-available by default', async () => {
      const statefulSet = await getKafkaStatefulSet();
      expect(statefulSet.spec.replicas).toBe(3);
    });

    it('should have default labels', async () => {
      const statefulSet = await getKafkaStatefulSet();

      const { metadata } = statefulSet;
      expect(metadata).toBeDefined();

      expect(metadata.labels).toEqual({
        'app.kubernetes.io/managed-by': 'Helm',
        'app.kubernetes.io/version': 'test-version',
        'helm.sh/chart': 'snow-white',
        'app.kubernetes.io/instance': 'test-release',
        'app.kubernetes.io/name': 'kafka',
        'app.kubernetes.io/part-of': 'snow-white',
      });
    });

    describe('replicas', () => {
      it("should deploy three replica in 'high-available' mode", async () => {
        const deployment = await getKafkaStatefulSet(
          await renderHelmChart({
            chartPath: 'charts/snow-white',
            values: {
              snowWhite: {
                mode: 'high-available',
              },
            },
          }),
        );

        expect(deployment.spec.replicas).toBe(3);
      });

      it("should deploy one replica in 'minimal' mode", async () => {
        const deployment = await getKafkaStatefulSet(
          await renderHelmChart({
            chartPath: 'charts/snow-white',
            values: {
              snowWhite: {
                mode: 'minimal',
              },
            },
          }),
        );

        expect(deployment.spec.replicas).toBe(1);
      });

      it("should deploy three replica in 'auto-scaling' mode", async () => {
        const deployment = await getKafkaStatefulSet(
          await renderHelmChart({
            chartPath: 'charts/snow-white',
            values: {
              snowWhite: {
                mode: 'auto-scaling',
              },
            },
          }),
        );

        expect(deployment.spec.replicas).toBe(3);
      });
    });
  });
});
