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
import { defaultLogPattern } from './constants';

describe('OTEL Event Filter Stream', () => {
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
          m.metadata.name ===
            'snow-white-otel-event-filter-stream-test-release',
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

      expect(metadata.name).toMatch(
        'snow-white-otel-event-filter-stream-test-release',
      );
    });

    it('should have default labels', async () => {
      const deployment = await renderAndGetDeployment();

      const { metadata } = deployment;
      expect(metadata).toBeDefined();

      expectToHaveDefaultLabelsForMicroservice(
        metadata.labels,
        'otel-event-filter-stream',
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
            'snow-white-otel-event-filter-stream-very-long-test-release-name',
      );

      expect(deployment).toBeDefined();
      expect(deployment.metadata.name).toHaveLength(63);
    });

    describe('replicas', () => {
      it('should deploy one replica by default', async () => {
        const deployment = await renderAndGetDeployment();

        expect(deployment.spec.replicas).toBe(1);
      });

      it('should override replica count from values', async () => {
        const replicas = 3;
        const deployment = await renderAndGetDeployment(
          await renderHelmChart({
            chartPath: 'charts/snow-white',
            values: {
              snowWhite: {
                otelEventFilterStream: {
                  replicas,
                },
              },
            },
          }),
        );

        expect(deployment.spec.replicas).toBe(replicas);
      });
    });

    describe('rollout', () => {
      it("should specify 'RollingUpdate' strategy", async () => {
        const deployment = await renderAndGetDeployment();

        const { spec } = deployment;
        expect(spec).toBeDefined();

        expect(spec.replicas).toBe(1);

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
          'app.kubernetes.io/component': 'otel-event-filter-stream',
          'app.kubernetes.io/instance': 'test-release',
          'app.kubernetes.io/name': 'otel-event-filter-stream',
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
          'otel-event-filter-stream',
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
                      'app.kubernetes.io/component': 'otel-event-filter-stream',
                      'app.kubernetes.io/instance': 'test-release',
                      'app.kubernetes.io/name': 'otel-event-filter-stream',
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
      it('should have only one (otel-event-filter-stream)', async () => {
        const templateSpec = getPodSpec(await renderAndGetDeployment());

        const { containers } = templateSpec;
        expect(containers).toHaveLength(1);

        expect(containers[0].name).toBe('otel-event-filter-stream');
      });

      describe('otel-event-filter-stream', () => {
        const renderAndGetOtelEventFilterStreamContainer = async (
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
            const otelEventFilterStream =
              await renderAndGetOtelEventFilterStreamContainer();

            expect(otelEventFilterStream.image).toBe(
              'ghcr.io/bbortt/snow-white/otel-event-filter-stream:v1.0.0',
            );
          });

          it('should adjust the image registry from values', async () => {
            const customRegistry = 'custom.registry';

            const otelEventFilterStream =
              await renderAndGetOtelEventFilterStreamContainer(
                await renderHelmChart({
                  chartPath: 'charts/snow-white',
                  values: {
                    snowWhite: {
                      image: { registry: customRegistry },
                    },
                  },
                }),
              );

            expect(otelEventFilterStream.image).toBe(
              'custom.registry/bbortt/snow-white/otel-event-filter-stream:v1.0.0',
            );
          });

          it('should adjust the image tag from values', async () => {
            const customTag = 'custom.tag';

            const otelEventFilterStream =
              await renderAndGetOtelEventFilterStreamContainer(
                await renderHelmChart({
                  chartPath: 'charts/snow-white',
                  values: {
                    snowWhite: {
                      otelEventFilterStream: { image: { tag: customTag } },
                    },
                  },
                }),
              );

            expect(otelEventFilterStream.image).toBe(
              'ghcr.io/bbortt/snow-white/otel-event-filter-stream:custom.tag',
            );
          });
        });

        describe('imagePullPolicy', () => {
          it('should pull images if they are not present by default', async () => {
            const otelEventFilterStream =
              await renderAndGetOtelEventFilterStreamContainer();

            expect(otelEventFilterStream.imagePullPolicy).toBe('Always');
          });

          it('should adjust the image pull policy from values', async () => {
            const imagePullPolicy = 'my.policy';

            const otelEventFilterStream =
              await renderAndGetOtelEventFilterStreamContainer(
                await renderHelmChart({
                  chartPath: 'charts/snow-white',
                  values: {
                    image: { pullPolicy: imagePullPolicy },
                  },
                }),
              );

            expect(otelEventFilterStream.imagePullPolicy).toBe(imagePullPolicy);
          });
        });

        describe('env', () => {
          it('should deploy 3+5 environment variables by default', async () => {
            const otelEventFilterStream =
              await renderAndGetOtelEventFilterStreamContainer();
            expect(otelEventFilterStream.env).toHaveLength(8);
          });

          it('should include default log pattern', async () => {
            const otelEventFilterStream =
              await renderAndGetOtelEventFilterStreamContainer();

            const protocol = otelEventFilterStream.env.find(
              (env) => env.name === 'LOGGING_PATTERN_CONSOLE',
            );
            expect(protocol).toBeDefined();
            expect(protocol.value).toBe(defaultLogPattern);
          });

          it('should include configuration for the OTEL collector', async () => {
            const otelEventFilterStream =
              await renderAndGetOtelEventFilterStreamContainer();

            const protocol = otelEventFilterStream.env.find(
              (env) => env.name === 'OTEL_EXPORTER_OTLP_PROTOCOL',
            );
            expect(protocol).toBeDefined();
            expect(protocol.value).toBe('grpc');

            const endpoint = otelEventFilterStream.env.find(
              (env) => env.name === 'OTEL_EXPORTER_OTLP_ENDPOINT',
            );
            expect(endpoint).toBeDefined();
            expect(endpoint.value).toBe(
              'http://snow-white-otel-collector-test-release.default.svc.cluster.local.:4317',
            );
          });

          it('should calculate api-index api base url', async () => {
            const openapiCoverageStream =
              await renderAndGetOtelEventFilterStreamContainer();

            const springDatasourceUrl = openapiCoverageStream.env.find(
              (env) =>
                env.name === 'SNOW_WHITE_OTEL_EVENT_FILTER_API-INDEX_BASE-URL',
            );
            expect(springDatasourceUrl).toBeDefined();

            expect(springDatasourceUrl.value).toBe(
              'snow-white-api-index-api-test-release.default.svc.cluster.local.:9092',
            );
          });

          it('should calculate kafka bootstrap servers by default', async () => {
            const otelEventFilterStream =
              await renderAndGetOtelEventFilterStreamContainer();

            const springDatasourceUrl = otelEventFilterStream.env.find(
              (env) => env.name === 'SPRING_KAFKA_BOOTSTRAP_SERVERS',
            );
            expect(springDatasourceUrl).toBeDefined();

            expect(springDatasourceUrl.value).toBe(
              'snow-white-kafka-connect-test-release.default.svc.cluster.local.:9092',
            );
          });

          it('should override kafka bootstrap servers from values', async () => {
            const brokers = 'brokers';
            const otelEventFilterStream =
              await renderAndGetOtelEventFilterStreamContainer(
                await renderHelmChart({
                  chartPath: 'charts/snow-white',
                  values: {
                    snowWhite: {
                      kafka: {
                        brokers,
                      },
                    },
                  },
                }),
              );

            const springDatasourceUrl = otelEventFilterStream.env.find(
              (env) => env.name === 'SPRING_KAFKA_BOOTSTRAP_SERVERS',
            );
            expect(springDatasourceUrl).toBeDefined();

            expect(springDatasourceUrl.value).toBe(brokers);
          });

          it.each([
            {
              otelCollector: {
                apiNameAttributeKey: 'custom-api-name',
              },
              envVarName:
                'SNOW_WHITE_OTEL_EVENT_FILTER_FILTERING_API-NAME-ATTRIBUTE-KEY',
              envVarValue: 'custom-api-name',
            },
            {
              otelCollector: {
                apiVersionAttributeKey: 'custom-api-version',
              },
              envVarName:
                'SNOW_WHITE_OTEL_EVENT_FILTER_FILTERING_API-VERSION-ATTRIBUTE-KEY',
              envVarValue: 'custom-api-version',
            },
            {
              otelCollector: {
                serviceNameAttributeKey: 'custom-service-name',
              },
              envVarName:
                'SNOW_WHITE_OTEL_EVENT_FILTER_FILTERING_SERVICE-NAME-ATTRIBUTE-KEY',
              envVarValue: 'custom-service-name',
            },
          ])(
            'should accept custom attribute key from values: $envVarName',
            async ({
              otelCollector,
              envVarName,
              envVarValue,
            }: {
              otelCollector: any;
              envVarName: string;
              envVarValue: string;
            }) => {
              const otelEventFilterStream =
                await renderAndGetOtelEventFilterStreamContainer(
                  await renderHelmChart({
                    chartPath: 'charts/snow-white',
                    values: {
                      otelCollector,
                    },
                  }),
                );

              // 1 Logging + 2 OTEL + 5 default + 1 custom configuration
              expect(otelEventFilterStream.env).toHaveLength(9);

              const customEnv = otelEventFilterStream.env.find(
                (env) => env.name === envVarName,
              );
              expect(customEnv).toBeDefined();
              expect(customEnv.value).toBe(envVarValue);
            },
          );

          it('should accept additional environment variables', async () => {
            const additionalEnvs = [
              { name: 'author', value: 'bbortt' },
              { name: 'foo', value: 'bar' },
            ];

            const otelEventFilterStream =
              await renderAndGetOtelEventFilterStreamContainer(
                await renderHelmChart({
                  chartPath: 'charts/snow-white',
                  values: {
                    snowWhite: {
                      otelEventFilterStream: { additionalEnvs },
                    },
                  },
                }),
              );

            // 1 Logging + 2 OTEL + 5 default + 2 additional
            expect(otelEventFilterStream.env).toHaveLength(10);

            const authorEnv = otelEventFilterStream.env.find(
              (env) => env.name === 'author',
            );
            expect(authorEnv).toBeDefined();
            expect(authorEnv.value).toBe('bbortt');

            const fooEnv = otelEventFilterStream.env.find(
              (env) => env.name === 'foo',
            );
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
          m.metadata.name ===
            'snow-white-otel-event-filter-stream-test-release',
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
            'snow-white-otel-event-filter-stream-very-long-test-release-name',
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
          'app.kubernetes.io/component': 'otel-event-filter-stream',
          'app.kubernetes.io/instance': 'test-release',
          'app.kubernetes.io/name': 'otel-event-filter-stream',
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
});
