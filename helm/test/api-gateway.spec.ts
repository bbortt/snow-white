/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { describe, it, expect } from 'vitest';
import { parseDocument } from 'yaml';
import { renderHelmChart } from './render-helm-chart';

const isSubset = (
  subset: Record<string, string>,
  superset: Record<string, any>,
): boolean => {
  for (const key in subset) {
    if (!(key in superset) || subset[key] !== superset[key]) {
      return false;
    }
  }

  return true;
};

describe('API Gateway', () => {
  describe('deployment', () => {
    const getApiGatewayDeployment = (manifests: any[]) => {
      const deployment = manifests.find(
        (m) =>
          m.kind === 'Deployment' &&
          m.metadata.name === 'snow-white-test-release-api-gateway',
      );
      expect(deployment).toBeDefined();
      return deployment;
    };

    const renderAndGetDeployment = async () => {
      const manifests = await renderHelmChart({
        chartPath: 'charts/snow-white',
      });

      const deployment = getApiGatewayDeployment(manifests);
      expect(deployment).toBeDefined();

      return deployment;
    };

    it('should be kubernetes Deployment', async () => {
      const deployment = await renderAndGetDeployment();

      expect(deployment.apiVersion).toMatch('v1');
      expect(deployment.kind).toMatch('Deployment');

      const { metadata } = deployment;
      expect(metadata).toBeDefined();

      expect(metadata.name).toMatch('snow-white-test-release-api-gateway');
    });

    it('should have default labels', async () => {
      const deployment = await renderAndGetDeployment();

      const { metadata } = deployment;
      expect(metadata).toBeDefined();

      expect(metadata.labels).toEqual({
        'app.kubernetes.io/managed-by': 'Helm',
        'app.kubernetes.io/version': 'test-version',
        'helm.sh/chart': 'snow-white',
        'app.kubernetes.io/instance': 'test-release',
        'app.kubernetes.io/name': 'api-gateway',
        'app.kubernetes.io/part-of': 'snow-white',
      });
    });

    it("should specify 'RollingUpdate' strategy", async () => {
      const deployment = await renderAndGetDeployment();

      const { spec } = deployment;
      expect(spec).toBeDefined();

      expect(spec.replicas).toBe(3);

      const { strategy } = spec;
      expect(strategy).toBeDefined();
      expect(strategy.type).toBe('RollingUpdate');
      expect(strategy.rollingUpdate).toBeDefined();
      expect(strategy.rollingUpdate.maxSurge).toBe(0);
      expect(strategy.rollingUpdate.maxUnavailable).toBe(1);
    });

    describe('replicas', () => {
      it("should deploy three replica in 'high-available' mode", async () => {
        const manifests = await renderHelmChart({
          chartPath: 'charts/snow-white',
          values: {
            snowWhite: {
              mode: 'high-available',
            },
          },
        });

        const deployment = getApiGatewayDeployment(manifests);

        expect(deployment.spec.replicas).toBe(3);
      });

      it("should deploy one replica in 'minimal' mode", async () => {
        const manifests = await renderHelmChart({
          chartPath: 'charts/snow-white',
          values: {
            snowWhite: {
              mode: 'minimal',
            },
          },
        });

        const deployment = getApiGatewayDeployment(manifests);

        expect(deployment.spec.replicas).toBe(1);
      });

      it("should not specify replicas in 'auto-scaling' mode", async () => {
        const manifests = await renderHelmChart({
          chartPath: 'charts/snow-white',
          values: {
            snowWhite: {
              mode: 'auto-scaling',
            },
          },
        });

        const deployment = getApiGatewayDeployment(manifests);

        expect(deployment.spec.replicas).toBeUndefined();
      });
    });

    describe('revisionHistoryLimit', () => {
      it('should be 3 by default', async () => {
        const deployment = await renderAndGetDeployment();

        const { spec } = deployment;
        expect(spec).toBeDefined();

        expect(spec.revisionHistoryLimit).toBe(3);
      });

      it('should adjust limit based on values', async () => {
        const revisionHistoryLimit = 1234;

        const manifests = await renderHelmChart({
          chartPath: 'charts/snow-white',
          values: {
            snowWhite: {
              revisionHistoryLimit,
            },
          },
        });

        const deployment = getApiGatewayDeployment(manifests);

        const { spec } = deployment;
        expect(spec).toBeDefined();

        expect(spec.revisionHistoryLimit).toBe(revisionHistoryLimit);
      });
    });

    describe('selector', () => {
      it('should contain immutable labels', async () => {
        const deployment = await renderAndGetDeployment();

        const { spec } = deployment;
        expect(spec).toBeDefined();

        const { selector } = spec;
        expect(selector).toBeDefined();

        expect(selector.matchLabels).toEqual({
          'app.kubernetes.io/instance': 'test-release',
          'app.kubernetes.io/name': 'api-gateway',
          'app.kubernetes.io/part-of': 'snow-white',
        });
      });

      it('should select correct pod based on selector labels', async () => {
        const deployment = await renderAndGetDeployment();

        const { template } = deployment;
        expect(template).toBeDefined();

        const { metadata } = template;
        expect(metadata).toBeDefined();

        expect(
          isSubset(deployment.spec.selector.matchLabels, metadata.labels),
        ).toBe(true);
      });
    });
  });

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

    const renderAndGetService = async () => {
      const manifests = await renderHelmChart({
        chartPath: 'charts/snow-white',
      });

      const service = getApiGatewayService(manifests);
      expect(service).toBeDefined();

      return service;
    };

    it('should be Kubernetes Service', async () => {
      const service = await renderAndGetService();

      expect(service.apiVersion).toMatch('v1');
      expect(service.kind).toMatch('Service');
      expect(service.metadata.name).toMatch(
        'snow-white-test-release-api-gateway',
      );
    });

    it('should have default labels', async () => {
      const service = await renderAndGetService();

      const { metadata } = service;
      expect(metadata).toBeDefined();

      expect(metadata.labels).toEqual({
        'app.kubernetes.io/managed-by': 'Helm',
        'app.kubernetes.io/version': 'test-version',
        'helm.sh/chart': 'snow-white',
        'app.kubernetes.io/instance': 'test-release',
        'app.kubernetes.io/name': 'api-gateway',
        'app.kubernetes.io/part-of': 'snow-white',
      });
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

    describe('type', async () => {
      it('should have default labels', async () => {
        const service = await renderAndGetService();

        const { spec } = service;
        expect(spec).toBeDefined();

        expect(spec.type).toBe('ClusterIP');
      });

      it('should be changeable with values', async () => {
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

        const { spec } = service;
        expect(spec).toBeDefined();

        expect(spec.type).toBe(customType);
      });
    });

    describe('ports', async () => {
      it('should map http port 80 by default', async () => {
        const service = await renderAndGetService();

        const { spec } = service;
        expect(spec).toBeDefined();

        expect(spec.ports).toHaveLength(1);
        const port = service.spec.ports[0];
        expect(port.name).toBe('http');
        expect(port.protocol).toBe('TCP');
        expect(port.port).toBe(80);
        expect(port.targetPort).toBe('http');
      });

      it('should configure dynamic port based on values', async () => {
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

    describe('selector', () => {
      it('should contain immutable labels', async () => {
        const service = await renderAndGetService();

        const { spec } = service;
        expect(spec).toBeDefined();

        expect(spec.selector).toEqual({
          'app.kubernetes.io/instance': 'test-release',
          'app.kubernetes.io/name': 'api-gateway',
          'app.kubernetes.io/part-of': 'snow-white',
        });
      });

      it('should select correct pod based on selector labels', async () => {
        const manifests = await renderHelmChart({
          chartPath: 'charts/snow-white',
        });

        const service = getApiGatewayService(manifests);

        const deployment = manifests.find(
          (m) =>
            m.kind === 'Deployment' &&
            isSubset(service.spec.selector, m.metadata?.labels),
        );

        expect(deployment).toBeDefined();
      });
    });
  });

  describe('ingress', () => {
    const getIngress = (manifests: any[]) => {
      const ingress = manifests.find(
        (m) =>
          m.kind === 'Ingress' && m.metadata.name === 'snow-white-test-release',
      );
      expect(ingress).toBeDefined();
      return ingress;
    };

    const renderAndGetIngress = async () => {
      const manifests = await renderHelmChart({
        chartPath: 'charts/snow-white',
      });

      const ingress = getIngress(manifests);
      expect(ingress).toBeDefined();

      return ingress;
    };

    it('should be kubernetes Ingress', async () => {
      const ingress = await renderAndGetIngress();

      expect(ingress.apiVersion).toMatch('v1');
      expect(ingress.kind).toMatch('Ingress');
      expect(ingress.metadata.name).toMatch('snow-white-test-release');
    });

    it('should have default labels', async () => {
      const ingress = await renderAndGetIngress();

      const { metadata } = ingress;
      expect(metadata).toBeDefined();

      expect(metadata.labels).toEqual({
        'app.kubernetes.io/managed-by': 'Helm',
        'app.kubernetes.io/version': 'test-version',
        'helm.sh/chart': 'snow-white',
        'app.kubernetes.io/instance': 'test-release',
        'app.kubernetes.io/name': 'api-gateway',
        'app.kubernetes.io/part-of': 'snow-white',
      });
    });

    it('should be the only exposed ingress', async () => {
      const manifests = await renderHelmChart({
        chartPath: 'charts/snow-white',
      });

      const ingress = manifests.filter((m) => m.kind === 'Ingress');
      expect(ingress).toBeDefined();
      expect(ingress).toHaveLength(1);
      expect(ingress[0].metadata.name).toBe('snow-white-test-release');
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

    it('can be disabled with values', async () => {
      const manifests = await renderHelmChart({
        chartPath: 'charts/snow-white',
        values: {
          snowWhite: {
            ingress: {
              enabled: false,
            },
          },
        },
      });

      const ingress = manifests.find(
        (m) =>
          m.kind === 'Ingress' && m.metadata.name === 'snow-white-test-release',
      );
      expect(ingress).toBeUndefined();
    });

    describe('annotations', () => {
      it('should not have any annotations by default', async () => {
        const ingress = await renderAndGetIngress();

        const { metadata } = ingress;
        expect(metadata).toBeDefined();

        expect(metadata.annotations).toBeUndefined();
      });

      it('renders with custom annotations based on values', async () => {
        const annotations = {
          'nginx.ingress.kubernetes.io/rewrite-target': '/',
          'nginx.ingress.kubernetes.io/ssl-redirect': 'false',
        };

        const manifests = await renderHelmChart({
          chartPath: 'charts/snow-white',
          values: {
            snowWhite: {
              ingress: {
                enabled: true,
                annotations,
              },
            },
          },
        });

        const ingress = getIngress(manifests);

        const { metadata } = ingress;
        expect(metadata).toBeDefined();

        expect(metadata.annotations).toEqual(annotations);
      });
    });

    describe('ingressClassName', () => {
      it('should be nginx ingress by default', async () => {
        const ingress = await renderAndGetIngress();

        const { spec } = ingress;
        expect(spec).toBeDefined();

        expect(spec.ingressClassName).toBe('nginx');
      });

      it('should render with custom ingress class name based on values', async () => {
        const ingressClassName = 'custom-nginx';

        const manifests = await renderHelmChart({
          chartPath: 'charts/snow-white',
          values: {
            snowWhite: {
              ingress: {
                className: ingressClassName,
              },
            },
          },
        });

        const ingress = getIngress(manifests);

        expect(ingress.spec.ingressClassName).toEqual(ingressClassName);
      });
    });

    describe('host mappings', () => {
      it('should map http port 80 by default', async () => {
        const ingress = await renderAndGetIngress();

        const { spec } = ingress;
        expect(spec).toBeDefined();

        expect(spec.tls).toHaveLength(1);
        const tls = spec.tls[0];
        expect(tls.hosts).toHaveLength(1);
        expect(tls.hosts[0]).toBe('localhost');

        expect(spec.rules).toHaveLength(1);
        const rule = spec.rules[0];
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

      it('should adjust hostname based on values', async () => {
        const hostname = 'not-localhost';

        const manifests = await renderHelmChart({
          chartPath: 'charts/snow-white',
          values: {
            snowWhite: {
              ingress: {
                host: hostname,
              },
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
});
