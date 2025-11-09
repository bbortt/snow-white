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
  describe('chart', () => {
    it('has enabled primary by default', async () => {
      const manifests = await renderHelmChart({
        chartPath: 'charts/snow-white',
      });

      const primaryStatefulSet = getPrimaryStatefulSet(manifests);
      expect(primaryStatefulSet.spec.template.spec.containers[0].image).toMatch(
        /^(registry-\d\.)?docker\.io\/bitnami\/postgresql:.+$/,
      );
    });

    it('has enabled read-replica by default', async () => {
      const manifests = await renderHelmChart({
        chartPath: 'charts/snow-white',
      });

      const readReplicas = getReadReplicas(manifests);
      expect(readReplicas.spec.template.spec.containers[0].image).toMatch(
        /^(registry-\d\.)?docker\.io\/bitnami\/postgresql:.+$/,
      );
      expect(readReplicas.spec.replicas).toBe(1);
    });

    it('can be disabled via properties', async () => {
      const manifests = await renderHelmChart({
        chartPath: 'charts/snow-white',
        values: {
          postgresql: {
            enabled: false,
          },
        },
      });

      const postgresqlResources = manifests.find((m) =>
        m.metadata.name.startsWith('test-release-postgresql'),
      );

      expect(postgresqlResources).toBeUndefined();
    });

    it('should be enhanced with password environment variables', async () => {
      const manifests = await renderHelmChart({
        chartPath: 'charts/snow-white',
      });

      const primaryStatefulSet = getPrimaryStatefulSet(manifests);

      const postgresqlContainer =
        primaryStatefulSet.spec.template.spec.containers.find(
          (container) => container.name === 'postgresql',
        );
      expect(postgresqlContainer).toBeDefined();

      const reportCoordinatorDatasourcePassword = postgresqlContainer.env.find(
        (env) => env.name === 'REPORT_COORDINATOR_DATASOURCE_PASSWORD',
      );
      expect(reportCoordinatorDatasourcePassword).toBeDefined();
      expect(
        reportCoordinatorDatasourcePassword.valueFrom.secretKeyRef.key,
      ).toBe('report-coord-password');
      expect(
        reportCoordinatorDatasourcePassword.valueFrom.secretKeyRef.name,
      ).toBe('snow-white-postgresql-credentials');

      const reportCoordinatorFlywayPassword = postgresqlContainer.env.find(
        (env) => env.name === 'REPORT_COORDINATOR_FLYWAY_PASSWORD',
      );
      expect(reportCoordinatorFlywayPassword).toBeDefined();
      expect(reportCoordinatorFlywayPassword.valueFrom.secretKeyRef.key).toBe(
        'report-coord-flyway-password',
      );
      expect(reportCoordinatorFlywayPassword.valueFrom.secretKeyRef.name).toBe(
        'snow-white-postgresql-credentials',
      );

      const qualityGateDatasourcePassword = postgresqlContainer.env.find(
        (env) => env.name === 'QUALITY_GATE_DATASOURCE_PASSWORD',
      );
      expect(qualityGateDatasourcePassword).toBeDefined();
      expect(qualityGateDatasourcePassword.valueFrom.secretKeyRef.key).toBe(
        'quality-gate-password',
      );
      expect(qualityGateDatasourcePassword.valueFrom.secretKeyRef.name).toBe(
        'snow-white-postgresql-credentials',
      );

      const qualityGateFlywayPassword = postgresqlContainer.env.find(
        (env) => env.name === 'QUALITY_GATE_FLYWAY_PASSWORD',
      );
      expect(qualityGateFlywayPassword).toBeDefined();
      expect(qualityGateFlywayPassword.valueFrom.secretKeyRef.key).toBe(
        'quality-gate-flyway-password',
      );
      expect(qualityGateFlywayPassword.valueFrom.secretKeyRef.name).toBe(
        'snow-white-postgresql-credentials',
      );
    });

    it('should have init scripts attached to it', async () => {
      const manifests = await renderHelmChart({
        chartPath: 'charts/snow-white',
      });

      const primaryStatefulSet = getPrimaryStatefulSet(manifests);

      const customInitScripts =
        primaryStatefulSet.spec.template.spec.volumes.find(
          (volume) => volume.name === 'custom-init-scripts',
        );
      expect(customInitScripts).toBeDefined();

      expect(customInitScripts.configMap.name).toBe(
        'snow-white-postgresql-init-scripts',
      );
    });
  });

  describe('secrets', () => {
    const renderAndGetPostgresqlSecret = async (manifests?: any[]) => {
      if (!manifests) {
        manifests = await renderHelmChart({
          chartPath: 'charts/snow-white',
        });
      }

      const secret = manifests.find(
        (m) =>
          m.kind === 'Secret' &&
          m.metadata.name === 'snow-white-postgresql-credentials',
      );
      expect(secret).toBeDefined();

      return secret;
    };

    it('should be kubernetes Secret', async () => {
      const secret = await renderAndGetPostgresqlSecret();

      expect(secret.apiVersion).toMatch('v1');
      expect(secret.kind).toMatch('Secret');

      const { metadata } = secret;
      expect(metadata).toBeDefined();

      expect(metadata.name).toMatch('snow-white-postgresql-credentials');
    });

    it('should have default labels', async () => {
      const secret = await renderAndGetPostgresqlSecret();

      const { metadata } = secret;
      expect(metadata).toBeDefined();

      expect(metadata.labels).toEqual({
        'app.kubernetes.io/managed-by': 'Helm',
        'app.kubernetes.io/version': 'test-version',
        'helm.sh/chart': 'snow-white',
        'app.kubernetes.io/instance': 'test-release',
        'app.kubernetes.io/name': 'postgresql',
        'app.kubernetes.io/part-of': 'snow-white',
      });
    });

    it('should truncate long release name', async () => {
      const manifests = await renderHelmChart({
        chartPath: 'charts/snow-white',
        // 53 chars is the max length for Helm release names
        releaseName: 'very-long-test-release-name-that-exceeds-the-limit',
      });

      const secret = manifests.find(
        (m) =>
          m.kind === 'Secret' &&
          m.metadata.name === 'snow-white-postgresql-credentials',
      );

      expect(secret).toBeDefined();
      expect(secret.metadata.name).toHaveLength(33); // static name
    });

    it('should define passwords for report-coordinator-api database users', async () => {
      const secret = await renderAndGetPostgresqlSecret();

      const { data } = secret;
      expect(data).toBeDefined();

      expect(data['report-coord-password']).toBeDefined();
      expect(data['report-coord-flyway-password']).toBeDefined();
    });

    it('should define passwords for quality-gate-api database users', async () => {
      const secret = await renderAndGetPostgresqlSecret();

      const { data } = secret;
      expect(data).toBeDefined();

      expect(data['quality-gate-password']).toBeDefined();
      expect(data['quality-gate-flyway-password']).toBeDefined();
    });

    it('should not be rendered when postgresql is disabled via properties', async () => {
      const manifests = await renderHelmChart({
        chartPath: 'charts/snow-white',
      });

      const secret = manifests.find(
        (m) =>
          m.kind === 'Secret' &&
          m.metadata.name === 'snow-white-postgresql-test-release',
      );
      expect(secret).toBeUndefined();
    });
  });

  describe('config map', () => {
    const renderAndGetConfigMap = async (manifests?: any[]) => {
      if (!manifests) {
        manifests = await renderHelmChart({
          chartPath: 'charts/snow-white',
        });
      }

      const configMap = manifests.find(
        (m) =>
          m.kind === 'ConfigMap' &&
          m.metadata.name === 'snow-white-postgresql-init-scripts',
      );
      expect(configMap).toBeDefined();

      return configMap;
    };

    it('should be kubernetes ConfigMap', async () => {
      const configMap = await renderAndGetConfigMap();

      expect(configMap.apiVersion).toMatch('v1');
      expect(configMap.kind).toMatch('ConfigMap');

      const { metadata } = configMap;
      expect(metadata).toBeDefined();

      expect(metadata.name).toMatch('snow-white-postgresql-init-scripts');
    });

    it('should define init script for report-coordinator-api', async () => {
      const configMap = await renderAndGetConfigMap();

      const { data } = configMap;
      expect(data).toBeDefined();
      expect(data['report-coordinator.sh']).toBeDefined();
    });

    it('should define init script for quality-gate-api', async () => {
      const configMap = await renderAndGetConfigMap();

      const { data } = configMap;
      expect(data).toBeDefined();
      expect(data['quality-gate.sh']).toBeDefined();
    });
  });
});
