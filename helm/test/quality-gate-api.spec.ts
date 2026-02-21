/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { describe, it, expect } from 'vitest';
import { parseDocument } from 'yaml';
import { renderHelmChart } from './render-helm-chart';
import {
  expectToHaveDefaultLabelsForMicroservice,
  getPodSpec,
  getTemplateMetadata,
  isSubset,
} from './helpers';
import { onPremDatasourceProperties } from './postgresql.spec';

describe('Quality-Gate API', () => {
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
          m.metadata.name === 'snow-white-quality-gate-api-test-release',
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

      expect(metadata.name).toMatch('snow-white-quality-gate-api-test-release');
    });

    it('should have default labels', async () => {
      const deployment = await renderAndGetDeployment();

      const { metadata } = deployment;
      expect(metadata).toBeDefined();

      expectToHaveDefaultLabelsForMicroservice(
        metadata.labels,
        'quality-gate-api',
      );
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
          'app.kubernetes.io/component': 'quality-gate-api',
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

    describe('template metadata', () => {
      describe('annotations', () => {
        it('should not have any annotations by default', async () => {
          const metadata = await getTemplateMetadata(
            await renderAndGetDeployment(),
          );

          expect(metadata.annotations).toBeNull();
        });

        it('should include optional annotations', async () => {
          const podAnnotations = {
            foo: 'bar',
          };

          const metadata = await getTemplateMetadata(
            await renderAndGetDeployment(
              await renderHelmChart({
                chartPath: 'charts/snow-white',
                values: {
                  snowWhite: {
                    podAnnotations,
                  },
                },
              }),
            ),
          );

          expect(metadata.annotations).toEqual(podAnnotations);
        });
      });

      it('should have default labels', async () => {
        const metadata = await getTemplateMetadata(
          await renderAndGetDeployment(),
        );

        expectToHaveDefaultLabelsForMicroservice(
          metadata.labels,
          'quality-gate-api',
        );
      });
    });

    describe('template spec', () => {
      describe('imagePullSecrets', () => {
        it('should have none by default', async () => {
          const deployment = await renderAndGetDeployment();

          const { spec } = deployment;
          expect(spec).toBeDefined();

          expect(spec.imagePullSecrets).toBeUndefined();
        });

        it('renders with custom image pull secret based on values', async () => {
          const token = 'something';
          const templateSpec = getPodSpec(
            await renderAndGetDeployment(
              await renderHelmChart({
                chartPath: 'charts/snow-white',
                values: {
                  global: {
                    imagePullSecrets: {
                      token,
                    },
                  },
                },
              }),
            ),
          );

          expect(templateSpec.imagePullSecrets).toEqual({ token });
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
                      'app.kubernetes.io/component': 'quality-gate-api',
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
              'ghcr.io/bbortt/snow-white/quality-gate-api:v1.0.0',
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
              'custom.registry/bbortt/snow-white/quality-gate-api:v1.0.0',
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
          it('should pull images if they are not present by default', async () => {
            const qualityGateApi = await renderAndGetQualityGateApiContainer();

            expect(qualityGateApi.imagePullPolicy).toBe('Always');
          });

          it('should adjust the image pull policy from values', async () => {
            const imagePullPolicy = 'my.policy';

            const qualityGateApi = await renderAndGetQualityGateApiContainer(
              await renderHelmChart({
                chartPath: 'charts/snow-white',
                values: {
                  image: { pullPolicy: imagePullPolicy },
                },
              }),
            );

            expect(qualityGateApi.imagePullPolicy).toBe(imagePullPolicy);
          });
        });

        describe('env', () => {
          it('should deploy 2+1+6 environment variables by default', async () => {
            const qualityGateApi = await renderAndGetQualityGateApiContainer();
            expect(qualityGateApi.env).toHaveLength(9);
          });

          it('should include configuration for the OTEL collector', async () => {
            const qualityGateApi = await renderAndGetQualityGateApiContainer();

            const protocol = qualityGateApi.env.find(
              (env) => env.name === 'OTEL_EXPORTER_OTLP_PROTOCOL',
            );
            expect(protocol).toBeDefined();
            expect(protocol.value).toBe('grpc');

            const endpoint = qualityGateApi.env.find(
              (env) => env.name === 'OTEL_EXPORTER_OTLP_ENDPOINT',
            );
            expect(endpoint).toBeDefined();
            expect(endpoint.value).toBe(
              'http://snow-white-otel-collector-test-release.default.svc.cluster.local.:4317',
            );
          });

          it('should include public host configuration with tls enabled', async () => {
            const qualityGateApi = await renderAndGetQualityGateApiContainer(
              await renderHelmChart({
                chartPath: 'charts/snow-white',
                values: {
                  snowWhite: {
                    ingress: {
                      host: 'custom-host',
                    },
                  },
                },
              }),
            );

            const publicApiGatewayUrl = qualityGateApi.env.find(
              (env) =>
                env.name ===
                'SNOW_WHITE_QUALITY_GATE_API_PUBLIC-API-GATEWAY-URL',
            );
            expect(publicApiGatewayUrl).toBeDefined();
            expect(publicApiGatewayUrl.value).toBe('https://custom-host');
          });

          it('should include public host configuration with tls disabled', async () => {
            const qualityGateApi = await renderAndGetQualityGateApiContainer(
              await renderHelmChart({
                chartPath: 'charts/snow-white',
                values: {
                  snowWhite: {
                    ingress: {
                      host: 'custom-host',
                      tls: false,
                    },
                  },
                },
              }),
            );

            const publicApiGatewayUrl = qualityGateApi.env.find(
              (env) =>
                env.name ===
                'SNOW_WHITE_QUALITY_GATE_API_PUBLIC-API-GATEWAY-URL',
            );
            expect(publicApiGatewayUrl).toBeDefined();
            expect(publicApiGatewayUrl.value).toBe('http://custom-host');
          });

          it('should calculate jdbc connection string', async () => {
            const qualityGateApi = await renderAndGetQualityGateApiContainer();

            const springDatasourceUrl = qualityGateApi.env.find(
              (env) => env.name === 'SPRING_DATASOURCE_URL',
            );
            expect(springDatasourceUrl).toBeDefined();

            expect(springDatasourceUrl.value).toBe(
              'jdbc:postgresql://test-release-postgresql.default.svc.cluster.local.:5432/quality-gate-api',
            );
          });

          it('should calculate spring datasource password', async () => {
            const manifests = await renderHelmChart({
              chartPath: 'charts/snow-white',
            });
            const qualityGateApi =
              await renderAndGetQualityGateApiContainer(manifests);

            const springDatasourcePassword = qualityGateApi.env.find(
              (env) => env.name === 'SPRING_DATASOURCE_PASSWORD',
            );
            expect(springDatasourcePassword).toBeDefined();

            expect(springDatasourcePassword.valueFrom.secretKeyRef.name).toBe(
              'snow-white-postgresql-credentials',
            );
            const secret = manifests.find(
              (m) =>
                m.kind === 'Secret' &&
                m.metadata.name ===
                  springDatasourcePassword.valueFrom.secretKeyRef.name,
            );
            expect(secret).toBeDefined();

            expect(springDatasourcePassword.valueFrom.secretKeyRef.key).toBe(
              'quality-gate-password',
            );
          });

          it('should calculate spring flyway password', async () => {
            const manifests = await renderHelmChart({
              chartPath: 'charts/snow-white',
            });
            const qualityGateApi =
              await renderAndGetQualityGateApiContainer(manifests);

            const springDatasourcePassword = qualityGateApi.env.find(
              (env) => env.name === 'SPRING_FLYWAY_PASSWORD',
            );
            expect(springDatasourcePassword).toBeDefined();

            expect(springDatasourcePassword.valueFrom.secretKeyRef.name).toBe(
              'snow-white-postgresql-credentials',
            );
            const secret = manifests.find(
              (m) =>
                m.kind === 'Secret' &&
                m.metadata.name ===
                  springDatasourcePassword.valueFrom.secretKeyRef.name,
            );
            expect(secret).toBeDefined();

            expect(springDatasourcePassword.valueFrom.secretKeyRef.key).toBe(
              'quality-gate-flyway-password',
            );
          });

          it('should accept additional environment variables', async () => {
            const additionalEnvs = [
              { name: 'author', value: 'bbortt' },
              { name: 'foo', value: 'bar' },
            ];

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

            // 2 OTEL + 1 JAVA_TOOL_OPTIONS + 6 default + 2 additional
            expect(qualityGateApi.env).toHaveLength(11);

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

        describe('with postgresql disabled', () => {
          it('should fail without defined SPRING_DATASOURCE_URL environment variable', async () => {
            await expect(
              renderHelmChart({
                chartPath: 'charts/snow-white',
                values: {
                  postgresql: {
                    enabled: false,
                  },
                  snowWhite: {
                    apiIndexApi: {
                      additionalEnvs: onPremDatasourceProperties,
                    },
                    reportCoordinatorApi: {
                      additionalEnvs: onPremDatasourceProperties,
                    },
                  },
                },
              }),
            ).rejects.toThrow(
              "Required environment variable 'SPRING_DATASOURCE_URL' is missing in snowWhite.qualityGateApi.additionalEnvs",
            );
          });

          it('should fail without defined SPRING_DATASOURCE_USERNAME environment variable', async () => {
            await expect(
              renderHelmChart({
                chartPath: 'charts/snow-white',
                values: {
                  postgresql: {
                    enabled: false,
                  },
                  snowWhite: {
                    apiIndexApi: {
                      additionalEnvs: onPremDatasourceProperties,
                    },
                    qualityGateApi: {
                      additionalEnvs: [
                        { name: 'SPRING_DATASOURCE_URL', value: 'value' },
                      ],
                    },
                    reportCoordinatorApi: {
                      additionalEnvs: onPremDatasourceProperties,
                    },
                  },
                },
              }),
            ).rejects.toThrow(
              "Required environment variable 'SPRING_DATASOURCE_USERNAME' is missing in snowWhite.qualityGateApi.additionalEnvs",
            );
          });

          it('should fail without defined SPRING_DATASOURCE_PASSWORD environment variable', async () => {
            await expect(
              renderHelmChart({
                chartPath: 'charts/snow-white',
                values: {
                  postgresql: {
                    enabled: false,
                  },
                  snowWhite: {
                    apiIndexApi: {
                      additionalEnvs: onPremDatasourceProperties,
                    },
                    qualityGateApi: {
                      additionalEnvs: [
                        { name: 'SPRING_DATASOURCE_URL', value: 'value' },
                        { name: 'SPRING_DATASOURCE_USERNAME', value: 'value' },
                      ],
                    },
                    reportCoordinatorApi: {
                      additionalEnvs: onPremDatasourceProperties,
                    },
                  },
                },
              }),
            ).rejects.toThrow(
              "Required environment variable 'SPRING_DATASOURCE_PASSWORD' is missing in snowWhite.qualityGateApi.additionalEnvs",
            );
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
          'app.kubernetes.io/component': 'quality-gate-api',
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

  describe('Service', () => {
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

    it('should be Kubernetes Service', async () => {
      const service = await renderAndGetQualityGateApiService();

      expect(service.apiVersion).toMatch('v1');
      expect(service.kind).toMatch('Service');
      expect(service.metadata.name).toMatch(
        'snow-white-quality-gate-api-test-release',
      );
    });

    it('should have default labels', async () => {
      const service = await renderAndGetQualityGateApiService();

      const { metadata } = service;
      expect(metadata).toBeDefined();

      expectToHaveDefaultLabelsForMicroservice(
        metadata.labels,
        'quality-gate-api',
      );
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
        const service = await renderAndGetQualityGateApiService();

        const { spec } = service;
        expect(spec).toBeDefined();

        expect(spec.selector).toEqual({
          'app.kubernetes.io/component': 'quality-gate-api',
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
