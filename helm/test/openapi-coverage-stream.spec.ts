/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { describe, it, expect } from 'vitest';
import { parseDocument } from 'yaml';
import { renderHelmChart } from './render-helm-chart';
import { isSubset } from './helpers';

describe('OpenAPI Coverage Stream', () => {
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
          m.metadata.name === 'snow-white-openapi-coverage-stream-test-release',
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

      expect(metadata.name).toMatch(
        'snow-white-openapi-coverage-stream-test-release',
      );
    });

    it('should have default labels', async () => {
      const deployment = await renderAndGetDeployment();

      const { metadata } = deployment;
      expect(metadata).toBeDefined();

      expect(metadata.labels).toEqual({
        'app.kubernetes.io/managed-by': 'Helm',
        'app.kubernetes.io/version': 'test-version',
        'helm.sh/chart': 'snow-white',
        'app.kubernetes.io/component': 'openapi-coverage-stream',
        'app.kubernetes.io/instance': 'test-release',
        'app.kubernetes.io/name': 'openapi-coverage-stream',
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
            'snow-white-openapi-coverage-stream-very-long-test-release-name',
      );

      expect(deployment).toBeDefined();
      expect(deployment.metadata.name).toHaveLength(62);
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
                openapiCoverageStream: {
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
          'app.kubernetes.io/component': 'openapi-coverage-stream',
          'app.kubernetes.io/instance': 'test-release',
          'app.kubernetes.io/name': 'openapi-coverage-stream',
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
                    'app.kubernetes.io/component': 'openapi-coverage-stream',
                    'app.kubernetes.io/instance': 'test-release',
                    'app.kubernetes.io/name': 'openapi-coverage-stream',
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
      it('should have only one (openapi-coverage-stream)', async () => {
        const templateSpec = getPodSpec(await renderAndGetDeployment());

        const { containers } = templateSpec;
        expect(containers).toHaveLength(1);

        expect(containers[0].name).toBe('openapi-coverage-stream');
      });

      describe('openapi-coverage-stream', () => {
        const renderAndGetReportCoordinatorApiContainer = async (
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
            const openapiCoverageStream =
              await renderAndGetReportCoordinatorApiContainer();

            expect(openapiCoverageStream.image).toBe(
              'ghcr.io/bbortt/snow-white/openapi-coverage-stream:v1.0.0-ci.0',
            );
          });

          it('should adjust the image registry from values', async () => {
            const customRegistry = 'custom.registry';

            const openapiCoverageStream =
              await renderAndGetReportCoordinatorApiContainer(
                await renderHelmChart({
                  chartPath: 'charts/snow-white',
                  values: {
                    snowWhite: {
                      image: { registry: customRegistry },
                    },
                  },
                }),
              );

            expect(openapiCoverageStream.image).toBe(
              'custom.registry/bbortt/snow-white/openapi-coverage-stream:v1.0.0-ci.0',
            );
          });

          it('should adjust the image tag from values', async () => {
            const customTag = 'custom.tag';

            const openapiCoverageStream =
              await renderAndGetReportCoordinatorApiContainer(
                await renderHelmChart({
                  chartPath: 'charts/snow-white',
                  values: {
                    snowWhite: {
                      openapiCoverageStream: { image: { tag: customTag } },
                    },
                  },
                }),
              );

            expect(openapiCoverageStream.image).toBe(
              'ghcr.io/bbortt/snow-white/openapi-coverage-stream:custom.tag',
            );
          });
        });

        describe('imagePullPolicy', () => {
          it('should pull images if they are not present by default', async () => {
            const openapiCoverageStream =
              await renderAndGetReportCoordinatorApiContainer();

            expect(openapiCoverageStream.imagePullPolicy).toBe('IfNotPresent');
          });

          it('should adjust the image pull policy from values', async () => {
            const imagePullPolicy = 'my.policy';

            const openapiCoverageStream =
              await renderAndGetReportCoordinatorApiContainer(
                await renderHelmChart({
                  chartPath: 'charts/snow-white',
                  values: {
                    image: { pullPolicy: imagePullPolicy },
                  },
                }),
              );

            expect(openapiCoverageStream.imagePullPolicy).toBe(imagePullPolicy);
          });
        });

        describe('env', () => {
          it('should deploy 2+9 environment variables by default', async () => {
            const openapiCoverageStream =
              await renderAndGetReportCoordinatorApiContainer();
            expect(openapiCoverageStream.env).toHaveLength(11);
          });

          it('should include configuration for the OTEL collector', async () => {
            const openapiCoverageStream =
              await renderAndGetReportCoordinatorApiContainer();

            const protocol = openapiCoverageStream.env.find(
              (env) => env.name === 'OTEL_EXPORTER_OTLP_PROTOCOL',
            );
            expect(protocol).toBeDefined();
            expect(protocol.value).toBe('grpc');

            const endpoint = openapiCoverageStream.env.find(
              (env) => env.name === 'OTEL_EXPORTER_OTLP_ENDPOINT',
            );
            expect(endpoint).toBeDefined();
            expect(endpoint.value).toBe(
              'http://snow-white-otel-collector-test-release.default.svc.cluster.local.:grpc',
            );
          });

          it('should calculate influxdb url by default', async () => {
            const openapiCoverageStream =
              await renderAndGetReportCoordinatorApiContainer();

            const influxdbUrl = openapiCoverageStream.env.find(
              (env) => env.name === 'INFLUXDB_URL',
            );
            expect(influxdbUrl).toBeDefined();

            expect(influxdbUrl.value).toBe(
              'test-release-influxdb.default.svc.cluster.local.:8086',
            );
          });

          it('should override influxdb url from values', async () => {
            const endpoint = 'endpoint';
            const openapiCoverageStream =
              await renderAndGetReportCoordinatorApiContainer(
                await renderHelmChart({
                  chartPath: 'charts/snow-white',
                  values: {
                    snowWhite: {
                      openapiCoverageStream: {
                        influxdb: {
                          endpoint,
                        },
                      },
                    },
                  },
                }),
              );

            const influxdbUrl = openapiCoverageStream.env.find(
              (env) => env.name === 'INFLUXDB_URL',
            );
            expect(influxdbUrl).toBeDefined();

            expect(influxdbUrl.value).toBe(endpoint);
          });

          it('should load influxdb token from secret by default', async () => {
            const openapiCoverageStream =
              await renderAndGetReportCoordinatorApiContainer();

            const influxdbUrl = openapiCoverageStream.env.find(
              (env) => env.name === 'INFLUXDB_TOKEN',
            );
            expect(influxdbUrl).toBeDefined();

            const secretKeyRef = influxdbUrl.valueFrom.secretKeyRef;
            expect(secretKeyRef).toBeDefined();

            expect(secretKeyRef.name).toBe('test-release-influxdb-auth');
            expect(secretKeyRef.key).toBe('influxdb-password');
          });

          it('should override influxdb token from values', async () => {
            const token = 'token';
            const openapiCoverageStream =
              await renderAndGetReportCoordinatorApiContainer(
                await renderHelmChart({
                  chartPath: 'charts/snow-white',
                  values: {
                    snowWhite: {
                      openapiCoverageStream: {
                        influxdb: {
                          token,
                        },
                      },
                    },
                  },
                }),
              );

            const influxdbUrl = openapiCoverageStream.env.find(
              (env) => env.name === 'INFLUXDB_TOKEN',
            );
            expect(influxdbUrl).toBeDefined();

            expect(influxdbUrl.value).toBe(token);
          });

          it('should calculate redis host by default', async () => {
            const openapiCoverageStream =
              await renderAndGetReportCoordinatorApiContainer();

            const springDatasourceUrl = openapiCoverageStream.env.find(
              (env) => env.name === 'SPRING_DATA_REDIS_HOST',
            );
            expect(springDatasourceUrl).toBeDefined();

            expect(springDatasourceUrl.value).toBe(
              'test-release-redis-master.default.svc.cluster.local.:6379',
            );
          });

          it('should override redis host from values', async () => {
            const host = 'host';
            const openapiCoverageStream =
              await renderAndGetReportCoordinatorApiContainer(
                await renderHelmChart({
                  chartPath: 'charts/snow-white',
                  values: {
                    snowWhite: {
                      openapiCoverageStream: {
                        redis: {
                          host,
                        },
                      },
                    },
                  },
                }),
              );

            const springDatasourceUrl = openapiCoverageStream.env.find(
              (env) => env.name === 'SPRING_DATA_REDIS_HOST',
            );
            expect(springDatasourceUrl).toBeDefined();

            expect(springDatasourceUrl.value).toBe(host);
          });

          it('should calculate kafka bootstrap servers by default', async () => {
            const openapiCoverageStream =
              await renderAndGetReportCoordinatorApiContainer();

            const springDatasourceUrl = openapiCoverageStream.env.find(
              (env) => env.name === 'SPRING_KAFKA_BOOTSTRAP_SERVERS',
            );
            expect(springDatasourceUrl).toBeDefined();

            expect(springDatasourceUrl.value).toBe(
              'snow-white-kafka-connect-test-release.default.svc.cluster.local.:9092',
            );
          });

          it('should override kafka bootstrap servers from values', async () => {
            const brokers = 'brokers';
            const openapiCoverageStream =
              await renderAndGetReportCoordinatorApiContainer(
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

            const springDatasourceUrl = openapiCoverageStream.env.find(
              (env) => env.name === 'SPRING_KAFKA_BOOTSTRAP_SERVERS',
            );
            expect(springDatasourceUrl).toBeDefined();

            expect(springDatasourceUrl.value).toBe(brokers);
          });

          it('should accept additional environment variables', async () => {
            const additionalEnvs = {
              author: 'bbortt',
              foo: 'bar',
            };

            const openapiCoverageStream =
              await renderAndGetReportCoordinatorApiContainer(
                await renderHelmChart({
                  chartPath: 'charts/snow-white',
                  values: {
                    snowWhite: {
                      openapiCoverageStream: { additionalEnvs },
                    },
                  },
                }),
              );

            // 2 OTEL + 9 default + 2 additional
            expect(openapiCoverageStream.env).toHaveLength(13);

            const authorEnv = openapiCoverageStream.env.find(
              (env) => env.name === 'author',
            );
            expect(authorEnv).toBeDefined();
            expect(authorEnv.value).toBe('bbortt');

            const fooEnv = openapiCoverageStream.env.find(
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
          m.metadata.name === 'snow-white-openapi-coverage-stream-test-release',
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
            'snow-white-openapi-coverage-stream-very-long-test-release-name',
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
          'app.kubernetes.io/component': 'openapi-coverage-stream',
          'app.kubernetes.io/instance': 'test-release',
          'app.kubernetes.io/name': 'openapi-coverage-stream',
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
