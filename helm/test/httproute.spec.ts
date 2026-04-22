/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { renderHelmChart } from './render-helm-chart';
import { expectToHaveDefaultLabelsForMicroservice } from './helpers';

const valueWithHttpRoute = {
  snowWhite: {
    httproute: {
      enabled: true,
    },
  },
};

describe('HttpRoute', () => {
  const renderAndGetHttpRoute = async (manifests?: any[]) => {
    if (!manifests) {
      manifests = await renderHelmChart({
        chartPath: 'charts/snow-white',
        values: valueWithHttpRoute,
      });
    }

    const httpRoute = manifests.find(
      (m) =>
        m.kind === 'HTTPRoute' && m.metadata.name === 'snow-white-test-release',
    );
    expect(httpRoute).toBeDefined();

    return httpRoute;
  };

  it('should be kubernetes HttpRoute', async () => {
    const httpRoute = await renderAndGetHttpRoute();

    expect(httpRoute.apiVersion).toMatch('v1');
    expect(httpRoute.kind).toMatch('HTTPRoute');
    expect(httpRoute.metadata.name).toMatch('snow-white-test-release');
  });

  it('should have default labels', async () => {
    const httpRoute = await renderAndGetHttpRoute();

    const { metadata } = httpRoute;
    expect(metadata).toBeDefined();

    expectToHaveDefaultLabelsForMicroservice(metadata.labels, 'httproute');
  });

  it('should be the only exposed httpRoute', async () => {
    const manifests = await renderHelmChart({
      chartPath: 'charts/snow-white',
      values: valueWithHttpRoute,
    });

    const httpRoute = manifests.filter((m) => m.kind === 'HTTPRoute');
    expect(httpRoute).toBeDefined();
    expect(httpRoute).toHaveLength(1);
    expect(httpRoute[0].metadata.name).toBe('snow-white-test-release');
  });

  it('truncates long release name', async () => {
    const manifests = await renderHelmChart({
      chartPath: 'charts/snow-white',
      // 53 chars is the max length for Helm release names
      releaseName: 'very-long-test-release-name-that-exceeds-the-limit-12',
      values: valueWithHttpRoute,
    });

    const httpRoute = manifests.find(
      (m) =>
        m.kind === 'HTTPRoute' &&
        m.metadata.name ===
          'snow-white-very-long-test-release-name-that-exceeds-the-limit-1',
    );

    expect(httpRoute).toBeDefined();
  });

  it('is disabled by default', async () => {
    await expect(() =>
      renderHelmChart({
        chartPath: 'charts/snow-white',
        withDefaultValues: false,
      }),
    ).rejects.toThrow(
      "ERROR: You must set one of 'snowWhite.httproute.enabled' or 'snowWhite.ingress.enabled'!",
    );
  });

  it('should throw error when HttpRoute is enabled but no public domain is set', async () => {
    await expect(() =>
      renderHelmChart({
        chartPath: 'charts/snow-white',
        values: {
          snowWhite: {
            httproute: {
              enabled: true,
            },
          },
        },
        withDefaultValues: false,
      }),
    ).rejects.toThrow(
      "ERROR: You must set 'snowWhite.host' to the public URL!",
    );
  });

  describe('annotations', () => {
    it('should have none by default', async () => {
      const httpRoute = await renderAndGetHttpRoute();

      const { metadata } = httpRoute;
      expect(metadata).toBeDefined();

      expect(metadata.annotations).toBeUndefined();
    });

    it('renders with custom annotations based on values', async () => {
      const annotations = {
        'nginx.httpRoute.kubernetes.io/rewrite-target': '/',
        'nginx.httpRoute.kubernetes.io/ssl-redirect': 'false',
      };

      const httpRoute = await renderAndGetHttpRoute(
        await renderHelmChart({
          chartPath: 'charts/snow-white',
          values: {
            snowWhite: {
              httproute: {
                enabled: true,
                annotations,
              },
            },
          },
        }),
      );

      const { metadata } = httpRoute;
      expect(metadata).toBeDefined();

      expect(metadata.annotations).toEqual(annotations);
    });
  });

  describe('parentRefs', () => {
    it('should not have any parentRefs by default', async () => {
      const httpRoute = await renderAndGetHttpRoute();

      expect(httpRoute.spec.parentRefs).toBeUndefined();
    });

    it('should include parentRefs from values', async () => {
      const name = 'acme-lb';
      const sectionName = 'foo';

      const httpRoute = await renderAndGetHttpRoute(
        await renderHelmChart({
          chartPath: 'charts/snow-white',
          values: {
            snowWhite: {
              host: 'hostname',
              httproute: {
                enabled: true,
                parentRefs: [{ name, sectionName }],
              },
            },
          },
        }),
      );

      expect(httpRoute.spec.parentRefs).toHaveLength(1);
      expect(httpRoute.spec.parentRefs[0].name).toBe(name);
      expect(httpRoute.spec.parentRefs[0].sectionName).toBe(sectionName);
    });
  });

  describe('host mappings', () => {
    it('should expose hostnames', async () => {
      const httpRoute = await renderAndGetHttpRoute();

      expect(httpRoute.spec.hostnames).toHaveLength(1);
      expect(httpRoute.spec.hostnames[0]).toBe('localhost');
    });

    it('should adjust hostname based on values', async () => {
      const hostname = 'not-localhost';

      const httpRoute = await renderAndGetHttpRoute(
        await renderHelmChart({
          chartPath: 'charts/snow-white',
          values: {
            snowWhite: {
              host: hostname,
            },
          },
        }),
      );

      expect(httpRoute.spec.hostnames).toHaveLength(1);
      expect(httpRoute.spec.hostnames[0]).toBe(hostname);
    });

    it('should include otel-collector port 4318 mapping by default', async () => {
      const httpRoute = await renderAndGetHttpRoute();

      const { spec } = httpRoute;
      expect(spec).toBeDefined();

      expect(spec.rules).toHaveLength(2);
      const rule = spec.rules[0];
      expect(rule.matches).toHaveLength(1);
      expect(rule.matches[0].path.type).toBe('PathPrefix');
      expect(rule.matches[0].path.value).toBe('/v1/traces');

      expect(rule.backendRefs).toHaveLength(1);
      expect(rule.backendRefs[0].name).toBe(
        'snow-white-otel-collector-test-release',
      );
      expect(rule.backendRefs[0].port).toBe(80);
    });

    it('should disable otel-collector port mapping with properties', async () => {
      const httpRoute = await renderAndGetHttpRoute(
        await renderHelmChart({
          chartPath: 'charts/snow-white',
          values: {
            ...valueWithHttpRoute,
            otelCollector: {
              exposeThroughApiGateway: false,
            },
          },
        }),
      );

      const { spec } = httpRoute;
      expect(spec).toBeDefined();

      expect(spec.rules).toHaveLength(1);
      const rule = spec.rules[0];

      expect(rule.matches).toHaveLength(1);
      expect(rule.matches[0].path.value).toBe('/');

      expect(rule.backendRefs).toHaveLength(1);
      expect(rule.backendRefs[0].name).toBe(
        'snow-white-api-gateway-test-release',
      );
      expect(rule.backendRefs[0].port).toBe(80);
    });

    it('should map to api-gateway port 80 by default', async () => {
      const httpRoute = await renderAndGetHttpRoute();

      const { spec } = httpRoute;
      expect(spec).toBeDefined();

      expect(spec.rules).toHaveLength(2);
      const rule = spec.rules[1];
      expect(rule.matches).toHaveLength(1);
      expect(rule.matches[0].path.type).toBe('PathPrefix');
      expect(rule.matches[0].path.value).toBe('/');

      expect(rule.backendRefs).toHaveLength(1);
      expect(rule.backendRefs[0].name).toBe(
        'snow-white-api-gateway-test-release',
      );
      expect(rule.backendRefs[0].port).toBe(80);
    });
  });
});
