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
import { defaultLogPattern } from './constants';

describe('MCP API', () => {
  describe('Deployment', () => {
    const renderAndGetDeployment = async (manifests?: any[]) => {
      if (!manifests) {
        manifests = await renderHelmChart({
          chartPath: 'charts/snow-white',
        });
      }

      const deployment = manifests.find(
        (m) =>
          m.kind === 'Deployment' &&
          m.metadata.name === 'snow-white-mcp-api-test-release',
      );
      expect(deployment).toBeDefined();

      return deployment;
    };

    it('should be kubernetes Deployment', async () => {
      const deployment = await renderAndGetDeployment();

      expect(deployment.apiVersion).toMatch('v1');
      expect(deployment.kind).toMatch('Deployment');

      const { metadata } = deployment;
      expect(metadata).toBeDefined();

      expect(metadata.name).toMatch('snow-white-mcp-api-test-release');
    });

    it('should have default labels', async () => {
      const deployment = await renderAndGetDeployment();

      const { metadata } = deployment;
      expect(metadata).toBeDefined();

      expectToHaveDefaultLabelsForMicroservice(metadata.labels, 'mcp-api');
    });

    describe('containers', () => {
      it('should have only one (mcp-api)', async () => {
        const templateSpec = getPodSpec(await renderAndGetDeployment());

        const { containers } = templateSpec;
        expect(containers).toHaveLength(1);

        expect(containers[0].name).toBe('mcp-api');
      });

      describe('mcp-api', () => {
        const renderAndGetMcpApiContainer = async (manifests?: any[]) => {
          const templateSpec = getPodSpec(
            await renderAndGetDeployment(manifests),
          );

          const { containers } = templateSpec;
          expect(containers).toBeDefined();

          return containers[0];
        };

        describe('image', () => {
          it('should be pulled from ghcr.io by default', async () => {
            const mcpApi = await renderAndGetMcpApiContainer();

            expect(mcpApi.image).toBe(
              'ghcr.io/bbortt/snow-white/mcp-api:v1.0.0',
            );
          });

          it('should adjust the image registry from values', async () => {
            const customRegistry = 'custom.registry';

            const mcpApi = await renderAndGetMcpApiContainer(
              await renderHelmChart({
                chartPath: 'charts/snow-white',
                values: {
                  snowWhite: {
                    image: { registry: customRegistry },
                  },
                },
              }),
            );

            expect(mcpApi.image).toBe(
              'custom.registry/bbortt/snow-white/mcp-api:v1.0.0',
            );
          });

          it('should adjust the image tag from values', async () => {
            const customTag = 'custom.tag';

            const mcpApi = await renderAndGetMcpApiContainer(
              await renderHelmChart({
                chartPath: 'charts/snow-white',
                values: {
                  snowWhite: {
                    mcpApi: { image: { tag: customTag } },
                  },
                },
              }),
            );

            expect(mcpApi.image).toBe(
              'ghcr.io/bbortt/snow-white/mcp-api:custom.tag',
            );
          });
        });

        describe('imagePullPolicy', () => {
          it('should pull images if they are not present by default', async () => {
            const mcpApi = await renderAndGetMcpApiContainer();

            expect(mcpApi.imagePullPolicy).toBe('Always');
          });

          it('should adjust the image pull policy from values', async () => {
            const imagePullPolicy = 'my.policy';

            const mcpApi = await renderAndGetMcpApiContainer(
              await renderHelmChart({
                chartPath: 'charts/snow-white',
                values: {
                  image: { pullPolicy: imagePullPolicy },
                },
              }),
            );

            expect(mcpApi.imagePullPolicy).toBe(imagePullPolicy);
          });
        });

        describe('env', () => {
          it('should deploy 3+1+3 environment variables by default', async () => {
            const mcpApi = await renderAndGetMcpApiContainer();
            expect(mcpApi.env).toHaveLength(7);
          });

          it('should include default log pattern', async () => {
            const mcpApi = await renderAndGetMcpApiContainer();

            const logPattern = mcpApi.env.find(
              (env) => env.name === 'LOGGING_PATTERN_CONSOLE',
            );
            expect(logPattern).toBeDefined();
            expect(logPattern.value).toBe(defaultLogPattern);
          });

          it('should include configuration for the OTEL collector', async () => {
            const mcpApi = await renderAndGetMcpApiContainer();

            const protocol = mcpApi.env.find(
              (env) => env.name === 'OTEL_EXPORTER_OTLP_PROTOCOL',
            );
            expect(protocol).toBeDefined();
            expect(protocol.value).toBe('grpc');

            const endpoint = mcpApi.env.find(
              (env) => env.name === 'OTEL_EXPORTER_OTLP_ENDPOINT',
            );
            expect(endpoint).toBeDefined();
            expect(endpoint.value).toBe(
              'http://snow-white-otel-collector-test-release.default.svc.cluster.local.:4317',
            );
          });

          it('should calculate backend connection strings based on release name', async () => {
            const mcpApi = await renderAndGetMcpApiContainer();

            const apiIndexUrl = mcpApi.env.find(
              (env) => env.name === 'SNOW_WHITE_MCP_API_API-INDEX_BASE-URL',
            );
            expect(apiIndexUrl).toBeDefined();
            expect(apiIndexUrl.value).toBe(
              'http://snow-white-api-index-api-test-release.default.svc.cluster.local.:80',
            );

            const criteriaUrl = mcpApi.env.find(
              (env) => env.name === 'SNOW_WHITE_MCP_API_CRITERIA_BASE-URL',
            );
            expect(criteriaUrl).toBeDefined();
            expect(criteriaUrl.value).toBe(
              'http://snow-white-quality-gate-api-test-release.default.svc.cluster.local.:80',
            );

            const reportUrl = mcpApi.env.find(
              (env) => env.name === 'SNOW_WHITE_MCP_API_REPORT_BASE-URL',
            );
            expect(reportUrl).toBeDefined();
            expect(reportUrl.value).toBe(
              'http://snow-white-report-coordinator-api-test-release.default.svc.cluster.local.:80',
            );
          });

          it('should accept additional environment variables', async () => {
            const additionalEnvs = [
              { name: 'author', value: 'bbortt' },
              { name: 'foo', value: 'bar' },
            ];

            const mcpApi = await renderAndGetMcpApiContainer(
              await renderHelmChart({
                chartPath: 'charts/snow-white',
                values: {
                  snowWhite: {
                    mcpApi: { additionalEnvs },
                  },
                },
              }),
            );

            expect(mcpApi.env).toHaveLength(9);

            const authorEnv = mcpApi.env.find((env) => env.name === 'author');
            expect(authorEnv).toBeDefined();
            expect(authorEnv.value).toBe('bbortt');

            const fooEnv = mcpApi.env.find((env) => env.name === 'foo');
            expect(fooEnv).toBeDefined();
            expect(fooEnv.value).toBe('bar');
          });
        });

        describe('ports', () => {
          it('should expose the MCP transport and the actuator', async () => {
            const mcpApi = await renderAndGetMcpApiContainer();

            expect(mcpApi.ports).toStrictEqual([
              { name: 'http', containerPort: 8080, protocol: 'TCP' },
              { name: 'actuator', containerPort: 8090, protocol: 'TCP' },
            ]);
          });
        });

        describe('probes', () => {
          it('should probe the actuator port', async () => {
            const mcpApi = await renderAndGetMcpApiContainer();

            expect(mcpApi.startupProbe.httpGet).toStrictEqual({
              scheme: 'HTTP',
              path: '/actuator/health',
              port: 'actuator',
            });
            expect(mcpApi.readinessProbe.httpGet.path).toBe(
              '/actuator/health/readiness',
            );
            expect(mcpApi.livenessProbe.httpGet.path).toBe(
              '/actuator/health/liveness',
            );
          });
        });

        describe('resources', () => {
          it('should be deployed default resource quota', async () => {
            const mcpApi = await renderAndGetMcpApiContainer();

            expect(mcpApi.resources).toStrictEqual({
              limits: {
                'ephemeral-storage': '2Gi',
                memory: '512Mi',
              },
              requests: {
                cpu: '0.2',
                'ephemeral-storage': '50Mi',
                memory: '512Mi',
              },
            });
          });

          it('should adjust resource request and limit based on values', async () => {
            const resources = {
              limits: {
                'ephemeral-storage': '1Gi',
                memory: '2Gi',
              },
              requests: {
                cpu: '3',
                'ephemeral-storage': '4Mi',
                memory: '5Gi',
              },
            };

            const mcpApi = await renderAndGetMcpApiContainer(
              await renderHelmChart({
                chartPath: 'charts/snow-white',
                values: {
                  snowWhite: {
                    mcpApi: { resources },
                  },
                },
              }),
            );

            expect(mcpApi.resources).toStrictEqual(resources);
          });
        });
      });
    });
  });

  describe('PodDisruptionBudget', () => {
    const renderAndGetPdb = async (manifests?: any[]) => {
      if (!manifests) {
        manifests = await renderHelmChart({
          chartPath: 'charts/snow-white',
        });
      }

      const pdb = manifests.find(
        (m) =>
          m.kind === 'PodDisruptionBudget' &&
          m.metadata.name === 'snow-white-mcp-api-test-release',
      );
      expect(pdb).toBeDefined();
      return pdb;
    };

    it('should select correct pod based on selector labels', async () => {
      const manifests = await renderHelmChart({
        chartPath: 'charts/snow-white',
      });

      const pdb = await renderAndGetPdb(manifests);

      const deployment = manifests.find(
        (m) =>
          m.kind === 'Deployment' &&
          isSubset(pdb.spec.selector.matchLabels, m.metadata?.labels),
      );

      expect(deployment).toBeDefined();
      expect(deployment.metadata.name).toBe('snow-white-mcp-api-test-release');
    });
  });

  describe('Service', () => {
    const renderAndGetMcpApiService = async (manifests?: any[]) => {
      if (!manifests) {
        manifests = await renderHelmChart({
          chartPath: 'charts/snow-white',
        });
      }

      const service = manifests.find(
        (m) =>
          m.kind === 'Service' &&
          m.metadata.name === 'snow-white-mcp-api-test-release',
      );
      expect(service).toBeDefined();
      return service;
    };

    it('should be an internal Kubernetes Service', async () => {
      const service = await renderAndGetMcpApiService();

      expect(service.apiVersion).toMatch('v1');
      expect(service.kind).toMatch('Service');

      expect(service.spec.type).toBe('ClusterIP');
      expect(service.spec.ports).toStrictEqual([
        { name: 'http', protocol: 'TCP', port: 80, targetPort: 'http' },
      ]);
    });

    it('should select correct pod based on selector labels', async () => {
      const manifests = await renderHelmChart({
        chartPath: 'charts/snow-white',
      });

      const service = await renderAndGetMcpApiService(manifests);

      const deployment = manifests.find(
        (m) =>
          m.kind === 'Deployment' &&
          isSubset(service.spec.selector, m.metadata?.labels),
      );

      expect(deployment).toBeDefined();
      expect(deployment.metadata.name).toBe('snow-white-mcp-api-test-release');
    });
  });
});
