/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { describe, it, expect } from 'vitest';
import { parseDocument } from 'yaml';
import { renderHelmChart } from './render-helm-chart';
import { isSubset } from './helpers';

describe('OTEL Collector', () => {
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
          m.metadata.name === 'snow-white-otel-collector-test-release',
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

      expect(metadata.name).toMatch('snow-white-otel-collector-test-release');
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
        'app.kubernetes.io/name': 'otel-collector',
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
            'snow-white-otel-collector-very-long-test-release-name-that-exce',
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
          'app.kubernetes.io/name': 'otel-collector',
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
                    'app.kubernetes.io/name': 'otel-collector',
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
      it('should have only one (otel-collector)', async () => {
        const templateSpec = getPodSpec(await renderAndGetDeployment());

        const { containers } = templateSpec;
        expect(containers).toHaveLength(1);

        expect(containers[0].name).toBe('otel-collector');
      });

      describe('otel-collector', () => {
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
          it('should be pulled from docker.io by default', async () => {
            const otelCollector = await renderAndGetQualityGateApiContainer();

            expect(otelCollector.image).toBe(
              'docker.io/otel/opentelemetry-collector-contrib:0.138.0',
            );
          });

          it('should adjust the image registry from values', async () => {
            const customRegistry = 'custom.registry';

            const otelCollector = await renderAndGetQualityGateApiContainer(
              await renderHelmChart({
                chartPath: 'charts/snow-white',
                values: {
                  snowWhite: {
                    otelCollector: { image: { registry: customRegistry } },
                  },
                },
              }),
            );

            expect(otelCollector.image).toBe(
              'custom.registry/otel/opentelemetry-collector-contrib:0.138.0',
            );
          });

          it('should adjust the image tag from values', async () => {
            const customTag = 'custom.tag';

            const otelCollector = await renderAndGetQualityGateApiContainer(
              await renderHelmChart({
                chartPath: 'charts/snow-white',
                values: {
                  snowWhite: {
                    otelCollector: { image: { tag: customTag } },
                  },
                },
              }),
            );

            expect(otelCollector.image).toBe(
              'docker.io/otel/opentelemetry-collector-contrib:custom.tag',
            );
          });
        });

        describe('imagePullPolicy', () => {
          it('should pull images if they are not present by default', async () => {
            const otelCollector = await renderAndGetQualityGateApiContainer();

            expect(otelCollector.imagePullPolicy).toBe('IfNotPresent');
          });

          it('should adjust the image pull policy from values', async () => {
            const imagePullPolicy = 'my.policy';

            const otelCollector = await renderAndGetQualityGateApiContainer(
              await renderHelmChart({
                chartPath: 'charts/snow-white',
                values: {
                  image: { pullPolicy: imagePullPolicy },
                },
              }),
            );

            expect(otelCollector.imagePullPolicy).toBe(imagePullPolicy);
          });
        });
      });
    });

    describe('volumes', () => {
      it('should map snow-white config volume', async () => {
        const manifests = await renderHelmChart({
          chartPath: 'charts/snow-white',
        });

        const deployment = manifests.find(
          (m) =>
            m.kind === 'Deployment' &&
            m.metadata.name === 'snow-white-otel-collector-test-release',
        );
        expect(deployment).toBeDefined();

        const { volumes } = deployment.spec.template.spec;
        expect(volumes).toBeDefined();

        const otelCollectorConfigVol = volumes.find(
          (volume) => volume.name === 'otel-collector-config-vol',
        );
        expect(otelCollectorConfigVol).toBeDefined();

        const correspondingConfigMap = manifests.find(
          (m) =>
            m.kind === 'ConfigMap' &&
            m.metadata.name === otelCollectorConfigVol.configMap.name,
        );
        expect(correspondingConfigMap).toBeDefined();
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
          m.metadata.name === 'snow-white-otel-collector-test-release',
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
            'snow-white-otel-collector-very-long-test-release-name-that-exce',
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
          'app.kubernetes.io/name': 'otel-collector',
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
          m.metadata.name === 'snow-white-otel-collector-test-release',
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
        'snow-white-otel-collector-test-release',
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
        'app.kubernetes.io/name': 'otel-collector',
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
            'snow-white-otel-collector-very-long-test-release-name-that-exce',
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
          'app.kubernetes.io/name': 'otel-collector',
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
