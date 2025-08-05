/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { describe, it, expect } from 'vitest';
import { parseDocument } from 'yaml';
import { renderHelmChart } from './render-helm-chart';

describe('API Gateway', () => {
  describe('service', () => {
    const getApiGatewayService = (manifests: any[]) => {
      const service = manifests.find(
        (m) =>
          m.kind === 'Service' &&
          m.metadata.name === 'snow-white-test-release-api-gateway',
      );
      expect(service).toBeDefined();
      return service;
    };

    it('renders with default values', async () => {
      const manifests = await renderHelmChart({
        chartPath: 'charts/snow-white',
      });

      const service = getApiGatewayService(manifests);
      expect(service.apiVersion).toMatch('v1');
      expect(service.kind).toMatch('Service');
      expect(service.metadata.name).toMatch(
        'snow-white-test-release-api-gateway',
      );

      expect(service.metadata.labels).toEqual({
        'app.kubernetes.io/managed-by': 'Helm',
        'app.kubernetes.io/version': 'v2.0.0',
        'helm.sh/chart': 'snow-white',
        'app.kubernetes.io/instance': 'test-release',
        'app.kubernetes.io/name': 'snow-white',
        'app.kubernetes.io/part-of': 'snow-white',
      });

      expect(service.spec.type).toBe('ClusterIP');

      expect(service.spec.selector).toEqual({
        'app.kubernetes.io/instance': 'test-release',
        'app.kubernetes.io/name': 'snow-white',
        'app.kubernetes.io/part-of': 'snow-white',
      });

      expect(service.spec.ports).toHaveLength(1);
      const port = service.spec.ports[0];
      expect(port.name).toBe('http');
      expect(port.protocol).toBe('TCP');
      expect(port.port).toBe(80);
      expect(port.targetPort).toBe('http');
    });

    it('has dynamic type based on values', async () => {
      const customType = 'custom';

      const manifests = await renderHelmChart({
        chartPath: 'charts/snow-white',
        values: {
          snowWhite: {
            apiGateway: {
              service: {
                type: customType,
              },
            },
          },
        },
      });

      const service = getApiGatewayService(manifests);
      expect(service.spec.type).toBe(customType);
    });

    it('has dynamic type based on values', async () => {
      const customPort = 8080;

      const manifests = await renderHelmChart({
        chartPath: 'charts/snow-white',
        values: {
          snowWhite: {
            apiGateway: {
              service: {
                port: customPort,
              },
            },
          },
        },
      });

      const service = getApiGatewayService(manifests);

      expect(service.spec.ports).toHaveLength(1);
      const port = service.spec.ports[0];
      expect(port.port).toBe(customPort);
    });
  });
});
