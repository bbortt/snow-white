/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { describe, it, expect } from 'vitest';
import { parseDocument } from 'yaml';
import { renderHelmChart } from './render-helm-chart';
import { isSubset } from './helpers';

describe('Quality-Gate API', () => {
  describe('deployment', () => {
    const renderAndGetDeployment = async (manifests?: any[]) => {
      if (!manifests) {
        manifests = await renderHelmChart({
          chartPath: 'charts/snow-white',
        });
      }

      const deployment = manifests.find(
        (m) =>
          m.kind === 'Deployment' &&
          m.metadata.name === 'snow-white-quality-gate-api-test-release',
      );
      expect(deployment).toBeDefined();

      return deployment;
    };

    const getPodSpec = (deployment) => {
      const { spec } = deployment;
      expect(spec).toBeDefined();

      const { template } = spec;
      expect(template).toBeDefined();

      const templateSpec = template.spec;
      expect(templateSpec).toBeDefined();

      return templateSpec;
    };

    it('should be kubernetes Deployment', async () => {
      const deployment = await renderAndGetDeployment();

      expect(deployment.apiVersion).toMatch('v1');
      expect(deployment.kind).toMatch('Deployment');

      const { metadata } = deployment;
      expect(metadata).toBeDefined();

      expect(metadata.name).toMatch('snow-white-quality-gate-api-test-release');
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
        'app.kubernetes.io/name': 'quality-gate-api',
        'app.kubernetes.io/part-of': 'snow-white',
      });
    });

    it('should truncate long release name', async () => {
      const manifests = await renderHelmChart({
        chartPath: 'charts/snow-white',
        // 53 chars is the max length for Helm release names
        releaseName: 'very-long-test-release-name-that-exceeds-the-limit',
      });

      const deployment = manifests.find(
        (m) =>
          m.kind === 'Deployment' &&
          m.metadata.name ===
            'snow-white-quality-gate-api-very-long-test-release-name-that-ex',
      );

      expect(deployment).toBeDefined();
      expect(deployment.metadata.name).toHaveLength(63);
    });

    describe('replicas', () => {
      it("should deploy three replica in 'high-available' mode", async () => {
        const deployment = await renderAndGetDeployment(
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
        const deployment = await renderAndGetDeployment(
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

      it("should not specify replicas in 'auto-scaling' mode", async () => {
        const deployment = await renderAndGetDeployment(
          await renderHelmChart({
            chartPath: 'charts/snow-white',
            values: {
              snowWhite: {
                mode: 'auto-scaling',
              },
            },
          }),
        );

        expect(deployment.spec.replicas).toBeUndefined();
      });
    });

    describe('rollout', () => {
      it("should specify 'RollingUpdate' strategy", async () => {
        const deployment = await renderAndGetDeployment();

        const { spec } = deployment;
        expect(spec).toBeDefined();

        expect(spec.replicas).toBe(3);

        const { strategy } = spec;
        expect(strategy).toBeDefined();
        expect(strategy.type).toBe('RollingUpdate');
        expect(strategy.rollingUpdate).toBeDefined();
        expect(strategy.rollingUpdate.maxSurge).toBe('25%');
        expect(strategy.rollingUpdate.maxUnavailable).toBe(0);
      });

      it('should be possible to disable max surge using values', async () => {
        const deployment = await renderAndGetDeployment(
          await renderHelmChart({
            chartPath: 'charts/snow-white',
            values: {
              snowWhite: {
                rollout: {
                  maxSurge: 0,
                },
              },
            },
          }),
        );

        const { spec } = deployment;
        expect(spec).toBeDefined();

        expect(spec.strategy.rollingUpdate.maxSurge).toBe(0);
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

        const deployment = await renderAndGetDeployment(
          await renderHelmChart({
            chartPath: 'charts/snow-white',
            values: {
              snowWhite: {
                revisionHistoryLimit,
              },
            },
          }),
        );

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
          'app.kubernetes.io/name': 'quality-gate-api',
          'app.kubernetes.io/part-of': 'snow-white',
        });
      });

      it('should select correct pod based on selector labels', async () => {
        const deployment = await renderAndGetDeployment();

        const { spec } = deployment;
        expect(spec).toBeDefined();

        const { template } = spec;
        expect(template).toBeDefined();

        const { metadata } = template;
        expect(metadata).toBeDefined();

        expect(
          isSubset(deployment.spec.selector.matchLabels, metadata.labels),
        ).toBe(true);
      });
    });

    describe('imagePullSecrets', () => {
      it('should have none by default', async () => {
        const deployment = await renderAndGetDeployment();

        const { spec } = deployment;
        expect(spec).toBeDefined();

        expect(spec.imagePullSecrets).toBeUndefined();
      });

      it('renders with custom image pull secret based on values', async () => {
        const templateSpec = getPodSpec(
          await renderAndGetDeployment(
            await renderHelmChart({
              chartPath: 'charts/snow-white',
              values: {
                global: {
                  imagePullSecrets: {
                    token: 'something',
                  },
                },
              },
            }),
          ),
        );

        expect(templateSpec.imagePullSecrets).toEqual({ token: 'something' });
      });
    });

    describe('affinities', () => {
      it('should define hostname pod anti affinity by default', async () => {
        const templateSpec = getPodSpec(await renderAndGetDeployment());

        expect(templateSpec.affinity.nodeAffinity).toBeNull();
        expect(templateSpec.affinity.podAffinity).toBeNull();
        expect(templateSpec.affinity.podAntiAffinity).toEqual({
          preferredDuringSchedulingIgnoredDuringExecution: [
            {
              podAffinityTerm: {
                labelSelector: {
                  matchLabels: {
                    'app.kubernetes.io/component': 'influxdb',
                    'app.kubernetes.io/instance': 'test-release',
                    'app.kubernetes.io/name': 'quality-gate-api',
                  },
                },
                topologyKey: 'kubernetes.io/hostname',
              },
              weight: 1,
            },
          ],
        });
      });

      it('should override all other affinities with custom affinity from values', async () => {
        const templateSpec = getPodSpec(
          await renderAndGetDeployment(
            await renderHelmChart({
              chartPath: 'charts/snow-white',
              values: {
                affinity: {
                  myAffinity: {
                    just: 'an example',
                  },
                },
              },
            }),
          ),
        );

        expect(templateSpec.affinity).toEqual({
          myAffinity: {
            just: 'an example',
          },
        });
      });
    });

    describe('containers', () => {
      it('should have only one (quality-gate-api)', async () => {
        const templateSpec = getPodSpec(await renderAndGetDeployment());

        const { containers } = templateSpec;
        expect(containers).toHaveLength(1);

        expect(containers[0].name).toBe('quality-gate-api');
      });

      describe('quality-gate-api', () => {
        const renderAndGetQualityGateApiContainer = async (
          manifests?: any[],
        ) => {
          const templateSpec = getPodSpec(
            await renderAndGetDeployment(manifests),
          );

          const { containers } = templateSpec;
          expect(containers).toBeDefined();

          return containers[0];
        };

        describe('image', () => {
          it('should be pulled from ghcr.io by default', async () => {
            const qualityGateApi = await renderAndGetQualityGateApiContainer();

            expect(qualityGateApi.image).toBe(
              'ghcr.io/bbortt/snow-white/quality-gate-api:v1.0.0-ci.0',
            );
          });

          it('should adjust the image registry from values', async () => {
            const customRegistry = 'custom.registry';

            const qualityGateApi = await renderAndGetQualityGateApiContainer(
              await renderHelmChart({
                chartPath: 'charts/snow-white',
                values: {
                  snowWhite: {
                    image: { registry: customRegistry },
                  },
                },
              }),
            );

            expect(qualityGateApi.image).toBe(
              'custom.registry/bbortt/snow-white/quality-gate-api:v1.0.0-ci.0',
            );
          });

          it('should adjust the image tag from values', async () => {
            const customTag = 'custom.tag';

            const qualityGateApi = await renderAndGetQualityGateApiContainer(
              await renderHelmChart({
                chartPath: 'charts/snow-white',
                values: {
                  snowWhite: {
                    qualityGateApi: { image: { tag: customTag } },
                  },
                },
              }),
            );

            expect(qualityGateApi.image).toBe(
              'ghcr.io/bbortt/snow-white/quality-gate-api:custom.tag',
            );
          });
        });

        describe('imagePullPolicy', () => {
          it('should always pull images by default', async () => {
            const qualityGateApi = await renderAndGetQualityGateApiContainer();

            expect(qualityGateApi.imagePullPolicy).toBe('Always');
          });

          it('should adjust the image pull policy from values', async () => {
            const imagePullPolicy = 'my.policy';

            const qualityGateApi = await renderAndGetQualityGateApiContainer(
              await renderHelmChart({
                chartPath: 'charts/snow-white',
                values: {
                  snowWhite: {
                    image: { pullPolicy: imagePullPolicy },
                  },
                },
              }),
            );

            expect(qualityGateApi.imagePullPolicy).toBe(imagePullPolicy);
          });
        });

        describe('env', () => {
          it('should deploy 6 environment variables by default', async () => {
            const qualityGateApi = await renderAndGetQualityGateApiContainer();
            expect(qualityGateApi.env).toHaveLength(6);
          });

          it('should calculate spring datasource password based on release name', async () => {
            const qualityGateApi = await renderAndGetQualityGateApiContainer();

            const springDatasourcePassword = qualityGateApi.env.find(
              (env) => env.name === 'SPRING_DATASOURCE_PASSWORD',
            );
            expect(springDatasourcePassword).toBeDefined();

            expect(springDatasourcePassword.valueFrom.secretKeyRef.name).toBe(
              'snow-white-postgresql-test-release',
            );
            expect(springDatasourcePassword.valueFrom.secretKeyRef.key).toBe(
              'quality-gate-password',
            );
          });

          it('should calculate spring flyway password based on release name', async () => {
            const qualityGateApi = await renderAndGetQualityGateApiContainer();

            const springDatasourcePassword = qualityGateApi.env.find(
              (env) => env.name === 'SPRING_FLYWAY_PASSWORD',
            );
            expect(springDatasourcePassword).toBeDefined();

            expect(springDatasourcePassword.valueFrom.secretKeyRef.name).toBe(
              'snow-white-postgresql-test-release',
            );
            expect(springDatasourcePassword.valueFrom.secretKeyRef.key).toBe(
              'quality-gate-flyway-password',
            );
          });

          it('should accept additional environment variables', async () => {
            const additionalEnvs = {
              author: 'bbortt',
              foo: 'bar',
            };

            const qualityGateApi = await renderAndGetQualityGateApiContainer(
              await renderHelmChart({
                chartPath: 'charts/snow-white',
                values: {
                  snowWhite: {
                    qualityGateApi: { additionalEnvs },
                  },
                },
              }),
            );

            // 6 default + 2 additional
            expect(qualityGateApi.env).toHaveLength(8);

            const authorEnv = qualityGateApi.env.find(
              (env) => env.name === 'author',
            );
            expect(authorEnv).toBeDefined();
            expect(authorEnv.value).toBe('bbortt');

            const fooEnv = qualityGateApi.env.find((env) => env.name === 'foo');
            expect(fooEnv).toBeDefined();
            expect(fooEnv.value).toBe('bar');
          });
        });
      });
    });
  });

  describe('pod disruption budget', () => {
    const renderAndGetPdb = async (manifests?: any[]) => {
      if (!manifests) {
        manifests = await renderHelmChart({
          chartPath: 'charts/snow-white',
        });
      }

      const pdb = manifests.find(
        (m) =>
          m.kind === 'PodDisruptionBudget' &&
          m.metadata.name === 'snow-white-quality-gate-api-test-release',
      );
      expect(pdb).toBeDefined();
      return pdb;
    };

    it('should truncate long release name', async () => {
      const manifests = await renderHelmChart({
        chartPath: 'charts/snow-white',
        // 53 chars is the max length for Helm release names
        releaseName: 'very-long-test-release-name-that-exceeds-the-limit-12',
      });

      const pdb = manifests.find(
        (m) =>
          m.kind === 'PodDisruptionBudget' &&
          m.metadata.name ===
            'snow-white-quality-gate-api-very-long-test-release-name-that-ex',
      );

      expect(pdb).toBeDefined();
    });

    describe('selector', () => {
      it('should contain immutable labels', async () => {
        const pdb = await renderAndGetPdb();

        const { spec } = pdb;
        expect(spec).toBeDefined();

        const { selector } = spec;
        expect(selector).toBeDefined();

        expect(selector.matchLabels).toEqual({
          'app.kubernetes.io/instance': 'test-release',
          'app.kubernetes.io/name': 'quality-gate-api',
          'app.kubernetes.io/part-of': 'snow-white',
        });
      });

      it('should select correct pod based on selector labels', async () => {
        const manifests = await renderHelmChart({
          chartPath: 'charts/snow-white',
        });

        const pdb = await renderAndGetPdb();

        const deployment = manifests.find(
          (m) =>
            m.kind === 'Deployment' &&
            isSubset(pdb.spec.selector.matchLabels, m.metadata?.labels),
        );

        expect(deployment).toBeDefined();
      });
    });
  });

  describe('service', () => {
    const renderAndGetQualityGateApiService = async (manifests?: any[]) => {
      if (!manifests) {
        manifests = await renderHelmChart({
          chartPath: 'charts/snow-white',
        });
      }

      const service = manifests.find(
        (m) =>
          m.kind === 'Service' &&
          m.metadata.name === 'snow-white-quality-gate-api-test-release',
      );
      expect(service).toBeDefined();
      return service;
    };

    const renderAndGetService = async () => {
      const service = await renderAndGetQualityGateApiService();
      expect(service).toBeDefined();

      return service;
    };

    it('should be Kubernetes Service', async () => {
      const service = await renderAndGetService();

      expect(service.apiVersion).toMatch('v1');
      expect(service.kind).toMatch('Service');
      expect(service.metadata.name).toMatch(
        'snow-white-quality-gate-api-test-release',
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
        'app.kubernetes.io/name': 'quality-gate-api',
        'app.kubernetes.io/part-of': 'snow-white',
      });
    });

    it('should truncate long release name', async () => {
      const manifests = await renderHelmChart({
        chartPath: 'charts/snow-white',
        // 53 chars is the max length for Helm release names
        releaseName: 'very-long-test-release-name-that-exceeds-the-limit',
      });

      const service = manifests.find(
        (m) =>
          m.kind === 'Service' &&
          m.metadata.name ===
            'snow-white-quality-gate-api-very-long-test-release-name-that-ex',
      );

      expect(service).toBeDefined();
      expect(service.metadata.name).toHaveLength(63);
    });

    describe('selector', () => {
      it('should contain immutable labels', async () => {
        const service = await renderAndGetService();

        const { spec } = service;
        expect(spec).toBeDefined();

        expect(spec.selector).toEqual({
          'app.kubernetes.io/instance': 'test-release',
          'app.kubernetes.io/name': 'quality-gate-api',
          'app.kubernetes.io/part-of': 'snow-white',
        });
      });

      it('should select correct pod based on selector labels', async () => {
        const manifests = await renderHelmChart({
          chartPath: 'charts/snow-white',
        });

        const service = await renderAndGetQualityGateApiService(manifests);

        const deployment = manifests.find(
          (m) =>
            m.kind === 'Deployment' &&
            isSubset(service.spec.selector, m.metadata?.labels),
        );

        expect(deployment).toBeDefined();
      });
    });
  });
});
