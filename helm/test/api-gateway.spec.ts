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

    it('truncates long release name', async () => {
      const manifests = await renderHelmChart({
        chartPath: 'charts/snow-white',
        // 53 chars is the max length for Helm release names
        releaseName: 'very-long-test-release-name-that-exceeds-the-limit',
      });

      const service = manifests.find(
        (m) =>
          m.kind === 'Service' &&
          m.metadata.name ===
            'snow-white-very-long-test-release-name-that-exceeds-the-limit-a',
      );

      expect(service).toBeDefined();
      expect(service.metadata.name).toHaveLength(63);
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

    it('has dynamic port based on values', async () => {
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

  describe('ingress', () => {
    const getIngress = (manifests: any[]) => {
      const service = manifests.find(
        (m) =>
          m.kind === 'Ingress' && m.metadata.name === 'snow-white-test-release',
      );
      expect(service).toBeDefined();
      return service;
    };

    it('renders with default values', async () => {
      const manifests = await renderHelmChart({
        chartPath: 'charts/snow-white',
      });

      const ingress = getIngress(manifests);
      expect(ingress.apiVersion).toMatch('v1');
      expect(ingress.kind).toMatch('Ingress');
      expect(ingress.metadata.name).toMatch('snow-white-test-release');

      expect(ingress.metadata.labels).toEqual({
        'app.kubernetes.io/managed-by': 'Helm',
        'app.kubernetes.io/version': 'v2.0.0',
        'helm.sh/chart': 'snow-white',
        'app.kubernetes.io/instance': 'test-release',
        'app.kubernetes.io/name': 'snow-white',
        'app.kubernetes.io/part-of': 'snow-white',
      });

      expect(ingress.metadata.annotations).toBeUndefined();

      expect(ingress.spec.ingressClassName).toBe('nginx');

      expect(ingress.spec.tls).toHaveLength(1);
      const tls = ingress.spec.tls[0];
      expect(tls.hosts).toHaveLength(1);
      expect(tls.hosts[0]).toBe('localhost');

      expect(ingress.spec.rules).toHaveLength(1);
      const rule = ingress.spec.rules[0];
      expect(rule.host).toBe('localhost');

      expect(rule.http.paths).toHaveLength(1);
      const path = rule.http.paths[0];
      expect(path.path).toBe('/');
      expect(path.pathType).toBe('Prefix');
      expect(path.backend.service.name).toBe(
        'snow-white-test-release-api-gateway',
      );
      expect(path.backend.service.port.name).toBe('http');
    });

    it('truncates long release name', async () => {
      const manifests = await renderHelmChart({
        chartPath: 'charts/snow-white',
        // 53 chars is the max length for Helm release names
        releaseName: 'very-long-test-release-name-that-exceeds-the-limit-12',
      });

      const ingress = manifests.find(
        (m) =>
          m.kind === 'Ingress' &&
          m.metadata.name ===
            'snow-white-very-long-test-release-name-that-exceeds-the-limit-1',
      );

      expect(ingress).toBeDefined();
    });

    it('can be disabled via properties', async () => {
      const manifests = await renderHelmChart({
        chartPath: 'charts/snow-white',
        values: {
          ingress: {
            enabled: false,
          },
        },
      });

      const ingress = manifests.find(
        (m) =>
          m.kind === 'Ingress' && m.metadata.name === 'snow-white-test-release',
      );
      expect(ingress).toBeUndefined();
    });

    it('renders with custom annotations based on values', async () => {
      const annotations = {
        'nginx.ingress.kubernetes.io/rewrite-target': '/',
        'nginx.ingress.kubernetes.io/ssl-redirect': 'false',
      };

      const manifests = await renderHelmChart({
        chartPath: 'charts/snow-white',
        values: {
          ingress: {
            enabled: true,
            annotations,
          },
        },
      });

      const ingress = getIngress(manifests);

      expect(ingress.metadata.annotations).toEqual(annotations);
    });

    it('renders with custom ingress class name based on values', async () => {
      const ingressClassName = 'custom-nginx';

      const manifests = await renderHelmChart({
        chartPath: 'charts/snow-white',
        values: {
          ingress: {
            className: ingressClassName,
          },
        },
      });

      const ingress = getIngress(manifests);

      expect(ingress.spec.ingressClassName).toEqual(ingressClassName);
    });

    it('renders with adjusted hostname based on values', async () => {
      const hostname = 'not-localhost';

      const manifests = await renderHelmChart({
        chartPath: 'charts/snow-white',
        values: {
          ingress: {
            host: hostname,
          },
        },
      });

      const ingress = getIngress(manifests);

      expect(ingress.spec.tls).toHaveLength(1);
      const tls = ingress.spec.tls[0];
      expect(tls.hosts).toHaveLength(1);
      expect(tls.hosts[0]).toBe(hostname);

      expect(ingress.spec.rules).toHaveLength(1);
      const rule = ingress.spec.rules[0];
      expect(rule.host).toBe(hostname);
    });
  });
});
