/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { describe, it, expect } from 'vitest';
import { renderHelmChart } from './render-helm-chart';

const getKafkaStatefulSet = (manifests: any[]) => {
  const statefulSet = manifests.find(
    (m) =>
      m.kind === 'StatefulSet' &&
      m.metadata.name.startsWith('test-release-kafka'),
  );
  expect(statefulSet).toBeDefined();
  return statefulSet;
};

describe('Kafka', () => {
  it('is enabled by default', async () => {
    const manifests = await renderHelmChart({
      chartPath: 'charts/snow-white',
    });

    const statefulSet = getKafkaStatefulSet(manifests);

    expect(statefulSet.spec.template.spec.containers[0].image).toMatch(
      /^docker.io\/bitnami\/kafka:.*$/,
    );
  });

  it('is high-available by default', async () => {
    const manifests = await renderHelmChart({
      chartPath: 'charts/snow-white',
    });

    const statefulSet = getKafkaStatefulSet(manifests);
    expect(statefulSet.spec.replicas).toBe(3);
  });

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
});
