/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { describe, it, expect } from 'vitest';
import { renderHelmChart } from './render-helm-chart';
import {
  expectToHaveDefaultLabelsForMicroservice,
  getPodSpec,
  isSubset,
} from './helpers';
import { parseAllDocuments } from 'yaml';

describe('Kafka UI', () => {
  it('is disabled by default', async () => {
    const manifests = await renderHelmChart({
      chartPath: 'charts/snow-white',
    });

    const kafkaUiResources = manifests.find((m) =>
      m.metadata.name.startsWith('test-release-kafka-ui'),
    );

    expect(kafkaUiResources).toBeUndefined();
  });

  it('can be enabled by value', async () => {
    const manifests = await renderHelmChart({
      chartPath: 'charts/snow-white',
      values: {
        ['kafka-ui']: {
          enabled: true,
        },
      },
    });

    const kafkaUiResources = manifests.filter((m) =>
      m.metadata.name.startsWith('test-release-kafka-ui'),
    );

    expect(kafkaUiResources).toHaveLength(3);
    expect(
      kafkaUiResources.find((r) => r.kind === 'ServiceAccount'),
    ).toBeDefined();
    expect(kafkaUiResources.find((r) => r.kind === 'Service')).toBeDefined();
    expect(kafkaUiResources.find((r) => r.kind === 'Deployment')).toBeDefined();

    const configMap = manifests.find(
      (m) =>
        m.kind === 'ConfigMap' &&
        m.metadata.name.startsWith('kafbat-ui-configmap'),
    );

    expect(configMap).toBeDefined();
  });

  it('should deploy configmap when enabled', async () => {
    const manifests = await renderHelmChart({
      chartPath: 'charts/snow-white',
      values: {
        ['kafka-ui']: {
          enabled: true,
        },
      },
    });

    const configMap = manifests.find(
      (m) =>
        m.kind === 'ConfigMap' &&
        m.metadata.name.startsWith('kafbat-ui-configmap'),
    );

    expect(configMap).toBeDefined();
  });

  const renderAndGetKafkaUiContainer = async (
    manifests?: any[],
  ): Promise<any> => {
    if (!manifests) {
      manifests = await renderHelmChart({
        chartPath: 'charts/snow-white',
        values: {
          ['kafka-ui']: {
            enabled: true,
          },
        },
      });
    }

    const deployment = manifests.find(
      (m) =>
        m.kind === 'Deployment' &&
        m.metadata.name.startsWith('test-release-kafka-ui'),
    );

    const { spec } = deployment;
    expect(spec).toBeDefined();

    const { template } = spec;
    expect(template).toBeDefined();

    const templateSpec = template.spec;
    expect(templateSpec).toBeDefined();

    const { containers, volumes } = templateSpec;
    expect(containers).toBeDefined();
    expect(containers).toHaveLength(1);

    const kafkaUiContainer = containers[0];
    return { kafkaUiContainer, volumes };
  };

  it('should mount configmap to container when enabled', async () => {
    const manifests = await renderHelmChart({
      chartPath: 'charts/snow-white',
      values: {
        ['kafka-ui']: {
          enabled: true,
        },
      },
    });

    const { kafkaUiContainer, volumes } =
      await renderAndGetKafkaUiContainer(manifests);

    expect(kafkaUiContainer.volumeMounts).toHaveLength(1);
    expect(kafkaUiContainer.volumeMounts[0].mountPath).toBe('/kafka-ui/');

    expect(volumes).toHaveLength(1);
    expect(volumes[0].name).toBe(kafkaUiContainer.volumeMounts[0].name);

    const configMap = manifests.find(
      (m) =>
        m.kind === 'ConfigMap' && m.metadata.name === volumes[0].configMap.name,
    );

    expect(configMap).toBeDefined();
  });

  it('should configure spring properties with configmap location when enabled', async () => {
    const { kafkaUiContainer } = await renderAndGetKafkaUiContainer();

    const additionalConfigLocation = kafkaUiContainer.env.find(
      (env) => env.name === 'SPRING_CONFIG_ADDITIONAL-LOCATION',
    );

    expect(additionalConfigLocation.value).toBe('/kafka-ui/config.yml');
  });

  describe('ConfigMap', () => {
    const renderAndGetConfigMap = async (): any => {
      const manifests = await renderHelmChart({
        chartPath: 'charts/snow-white',
        values: {
          ['kafka-ui']: {
            enabled: true,
          },
        },
      });

      const configMap = manifests.find(
        (m) =>
          m.kind === 'ConfigMap' &&
          m.metadata.name.startsWith('kafbat-ui-configmap'),
      );

      expect(configMap).toBeDefined();
      return configMap;
    };

    it('should not be deployed by default', async () => {
      const manifests = await renderHelmChart({
        chartPath: 'charts/snow-white',
      });

      const configMap = manifests.find(
        (m) =>
          m.kind === 'ConfigMap' &&
          m.metadata.name.startsWith('kafbat-ui-configmap'),
      );

      expect(configMap).toBeUndefined();
    });

    it('should be deployed when kafka-ui is enabled', async () => {
      const configMap = await renderAndGetConfigMap();
      expect(configMap).toBeDefined();
    });

    const extractConfigMapData = (data): any => {
      const snowWhiteConfigDataName = 'config.yml';
      expect(data).toHaveProperty(snowWhiteConfigDataName);

      const docs = parseAllDocuments(data[snowWhiteConfigDataName]);
      const json = docs.map((doc) => doc.toJSON()).filter(Boolean);
      expect(json).toHaveLength(1);

      return json[0];
    };

    it('should calculate bootstrap server url', async () => {
      const configMap = await renderAndGetConfigMap();

      const { data } = configMap;
      expect(data).toBeDefined();

      const snowWhiteConfig = extractConfigMapData(data);

      expect(snowWhiteConfig.kafka.clusters).toHaveLength(1);
      expect(snowWhiteConfig.kafka.clusters[0].name).toBe('Snow-White');
      expect(snowWhiteConfig.kafka.clusters[0].bootstrapServers).toBe(
        'snow-white-kafka-connect-test-release.default.svc.cluster.local.:9092',
      );
    });
  });
});
