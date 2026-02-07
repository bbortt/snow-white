/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
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

const renderAndGetKafkaStatefulSet = async (manifests?: any[]) => {
  if (!manifests) {
    manifests = await renderHelmChart({
      chartPath: 'charts/snow-white',
    });
  }

  const statefulSet = manifests.find(
    (m) =>
      m.kind === 'StatefulSet' &&
      m.metadata.name === 'snow-white-kafka-test-release',
  );
  expect(statefulSet).toBeDefined();

  return statefulSet;
};

describe('Kafka', () => {
  it('can be disabled with values', async () => {
    const manifests = await renderHelmChart({
      chartPath: 'charts/snow-white',
      values: {
        kafka: {
          enabled: false,
        },
      },
    });

    const kafkaResources = manifests.find((m) =>
      m.metadata.name.startsWith('test-release-kafka'),
    );

    expect(kafkaResources).toBeUndefined();
  });

  describe('StatefulSet', () => {
    it('is enabled by default', async () => {
      const statefulSet = await renderAndGetKafkaStatefulSet();

      expect(statefulSet.spec.template.spec.containers[0].image).toMatch(
        /^(registry-\d\.)?docker\.io\/confluentinc\/cp-kafka:.+$/,
      );
    });

    it('is high-available by default', async () => {
      const statefulSet = await renderAndGetKafkaStatefulSet();
      expect(statefulSet.spec.replicas).toBe(3);
    });

    it('should have default labels', async () => {
      const statefulSet = await renderAndGetKafkaStatefulSet();

      const { metadata } = statefulSet;
      expect(metadata).toBeDefined();

      expectToHaveDefaultLabelsForMicroservice(metadata.labels, 'kafka');
    });

    describe('replicas', () => {
      it("should deploy three replica in 'high-available' mode", async () => {
        const statefulSet = await renderAndGetKafkaStatefulSet(
          await renderHelmChart({
            chartPath: 'charts/snow-white',
            values: {
              snowWhite: {
                mode: 'high-available',
              },
            },
          }),
        );

        expect(statefulSet.spec.replicas).toBe(3);
      });

      it("should deploy one replica in 'minimal' mode", async () => {
        const statefulSet = await renderAndGetKafkaStatefulSet(
          await renderHelmChart({
            chartPath: 'charts/snow-white',
            values: {
              snowWhite: {
                mode: 'minimal',
              },
            },
          }),
        );

        expect(statefulSet.spec.replicas).toBe(1);
      });

      it("should deploy three replica in 'auto-scaling' mode", async () => {
        const statefulSet = await renderAndGetKafkaStatefulSet(
          await renderHelmChart({
            chartPath: 'charts/snow-white',
            values: {
              snowWhite: {
                mode: 'auto-scaling',
              },
            },
          }),
        );

        expect(statefulSet.spec.replicas).toBe(3);
      });
    });

    describe('Service', () => {
      it('should have specified headless service name for direct communication', async () => {
        const manifests = await renderHelmChart({
          chartPath: 'charts/snow-white',
        });
        const statefulSet = await renderAndGetKafkaStatefulSet(manifests);

        expect(statefulSet.spec.serviceName).toBe(
          'snow-white-kafka-test-release',
        );

        expect(
          manifests.find(
            (m) =>
              m.kind === 'Service' &&
              m.metadata.name === statefulSet.spec.serviceName,
          ),
        ).toBeDefined();
      });
    });

    describe('selector', () => {
      it('should contain immutable labels', async () => {
        const statefulSet = await renderAndGetKafkaStatefulSet();

        const { spec } = statefulSet;
        expect(spec).toBeDefined();

        const { selector } = spec;
        expect(selector).toBeDefined();

        expect(selector.matchLabels).toEqual({
          'app.kubernetes.io/component': 'kafka',
          'app.kubernetes.io/instance': 'test-release',
          'app.kubernetes.io/name': 'kafka',
          'app.kubernetes.io/part-of': 'snow-white',
        });
      });

      it('should select correct pod based on selector labels', async () => {
        const statefulSet = await renderAndGetKafkaStatefulSet();

        const { spec } = statefulSet;
        expect(spec).toBeDefined();

        const { template } = spec;
        expect(template).toBeDefined();

        const { metadata } = template;
        expect(metadata).toBeDefined();

        expect(
          isSubset(statefulSet.spec.selector.matchLabels, metadata.labels),
        ).toBe(true);
      });
    });

    describe('imagePullSecrets', () => {
      it('should have none by default', async () => {
        const statefulSet = await renderAndGetKafkaStatefulSet();

        const { spec } = statefulSet;
        expect(spec).toBeDefined();

        expect(spec.imagePullSecrets).toBeUndefined();
      });

      it('renders with custom image pull secret based on values', async () => {
        const token = 'something';
        const statefulSet = await renderAndGetKafkaStatefulSet(
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
        );

        const templateSpec = getPodSpec(statefulSet);

        expect(templateSpec.imagePullSecrets).toEqual({ token });
      });
    });

    describe('affinities', () => {
      it('should define hostname pod anti affinity by default', async () => {
        const templateSpec = getPodSpec(await renderAndGetKafkaStatefulSet());

        expect(templateSpec.affinity.nodeAffinity).toBeNull();
        expect(templateSpec.affinity.podAffinity).toBeNull();
        expect(templateSpec.affinity.podAntiAffinity).toEqual({
          preferredDuringSchedulingIgnoredDuringExecution: [
            {
              podAffinityTerm: {
                labelSelector: {
                  matchLabels: {
                    'app.kubernetes.io/component': 'kafka',
                    'app.kubernetes.io/instance': 'test-release',
                    'app.kubernetes.io/name': 'kafka',
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
          await renderAndGetKafkaStatefulSet(
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
      it('should have only one (kafka)', async () => {
        const templateSpec = getPodSpec(await renderAndGetKafkaStatefulSet());

        const { containers } = templateSpec;
        expect(containers).toHaveLength(1);

        expect(containers[0].name).toBe('kafka');
      });

      describe('kafka', () => {
        const renderAndGetKafkaContainer = async (manifests?: any[]) => {
          const templateSpec = getPodSpec(
            await renderAndGetKafkaStatefulSet(manifests),
          );

          const { containers } = templateSpec;
          expect(containers).toBeDefined();

          return containers[0];
        };

        describe('image', () => {
          it('should be pulled from docker.io by default', async () => {
            const kafka = await renderAndGetKafkaContainer();

            expect(kafka.image).toMatch(
              /^docker\.io\/confluentinc\/cp-kafka:.+$/,
            );
          });

          it('should adjust the container registry from values', async () => {
            const customRegistry = 'custom.registry';

            const kafka = await renderAndGetKafkaContainer(
              await renderHelmChart({
                chartPath: 'charts/snow-white',
                values: {
                  kafka: {
                    image: { registry: customRegistry },
                  },
                },
              }),
            );

            expect(kafka.image).toMatch(
              /^custom\.registry\/confluentinc\/cp-kafka:.+$/,
            );
          });

          it('should adjust the image name from values', async () => {
            const customName = 'other-kafka';

            const kafka = await renderAndGetKafkaContainer(
              await renderHelmChart({
                chartPath: 'charts/snow-white',
                values: {
                  kafka: { image: { name: customName } },
                },
              }),
            );

            expect(kafka.image).toMatch(/^docker\.io\/other-kafka:.+$/);
          });

          it('should adjust the image tag from values', async () => {
            const customTag = 'custom.tag';

            const kafka = await renderAndGetKafkaContainer(
              await renderHelmChart({
                chartPath: 'charts/snow-white',
                values: {
                  kafka: { image: { tag: customTag } },
                },
              }),
            );

            expect(kafka.image).toBe(
              'docker.io/confluentinc/cp-kafka:custom.tag',
            );
          });
        });

        describe('imagePullPolicy', () => {
          it('should pull images if they are not present by default', async () => {
            const kafka = await renderAndGetKafkaContainer();

            expect(kafka.imagePullPolicy).toBe('Always');
          });

          it('should adjust the image pull policy from values', async () => {
            const imagePullPolicy = 'my.policy';

            const kafka = await renderAndGetKafkaContainer(
              await renderHelmChart({
                chartPath: 'charts/snow-white',
                values: {
                  image: { pullPolicy: imagePullPolicy },
                },
              }),
            );

            expect(kafka.imagePullPolicy).toBe(imagePullPolicy);
          });
        });

        describe('env', () => {
          it('should deploy 1+9+1+1 environment variables by default', async () => {
            const kafka = await renderAndGetKafkaContainer();
            expect(kafka.env).toHaveLength(12);
          });

          it('should calculate KAFKA_ADVERTISED_LISTENERS', async () => {
            const kafka = await renderAndGetKafkaContainer();

            const advertisedListeners = kafka.env.find(
              (env) => env.name === 'KAFKA_ADVERTISED_LISTENERS',
            );
            expect(advertisedListeners).toBeDefined();
            expect(advertisedListeners.value).toBe(
              'INTERNAL://$(POD_NAME).snow-white-kafka-test-release.default.svc.cluster.local.:9092',
            );
          });

          const fullQuorumVoters =
            '0@snow-white-kafka-test-release-0.snow-white-kafka-test-release.default.svc.cluster.local.:9093,1@snow-white-kafka-test-release-1.snow-white-kafka-test-release.default.svc.cluster.local.:9093,2@snow-white-kafka-test-release-2.snow-white-kafka-test-release.default.svc.cluster.local.:9093';

          it.each([
            {
              mode: 'minimal',
              quorumVoters:
                '0@snow-white-kafka-test-release-0.snow-white-kafka-test-release.default.svc.cluster.local.:9093',
            },
            { mode: 'high-available', quorumVoters: fullQuorumVoters },
            { mode: 'auto-scaling', quorumVoters: fullQuorumVoters },
          ])(
            'should calculate KAFKA_CONTROLLER_QUORUM_VOTERS: %s',
            async ({ mode, quorumVoters }) => {
              const kafka = await renderAndGetKafkaContainer(
                await renderHelmChart({
                  chartPath: 'charts/snow-white',
                  values: {
                    snowWhite: {
                      mode,
                    },
                  },
                }),
              );

              const kafkaQuorumVoters = kafka.env.find(
                (env) => env.name === 'KAFKA_CONTROLLER_QUORUM_VOTERS',
              );
              expect(kafkaQuorumVoters).toBeDefined();
              expect(kafkaQuorumVoters.value).toBe(quorumVoters);
            },
          );
        });

        it('should mount defined volume', async () => {
          const kafka = await renderAndGetKafkaContainer();

          expect(kafka.volumeMounts).toHaveLength(1);
          expect(kafka.volumeMounts[0].name).toBe('datadir');
        });
      });
    });

    describe('volumes', () => {
      it('should map pvc', async () => {
        const manifests = await renderHelmChart({
          chartPath: 'charts/snow-white',
        });
        const statefulSet = await renderAndGetKafkaStatefulSet(manifests);

        const { spec } = statefulSet;
        expect(spec).toBeDefined();

        const { template } = spec;
        expect(template).toBeDefined();

        const templateSpec = template.spec;
        expect(templateSpec).toBeDefined();

        const { volumes } = templateSpec;
        expect(volumes).toBeDefined();
        expect(volumes).toHaveLength(1);

        const datadir = volumes[0];
        expect(datadir.name).toBe('datadir');

        expect(
          manifests.find(
            (m) =>
              m.kind === 'PersistentVolumeClaim' &&
              m.metadata.name === datadir.persistentVolumeClaim.claimName,
          ),
        ).toBeDefined();
      });
    });
  });

  describe('PersistentVolumeClaim', () => {
    const renderAndGetPvc = async (values?: object): Promise<any> => {
      const manifests = await renderHelmChart({
        chartPath: 'charts/snow-white',
        values,
      });

      const pvc = manifests.find(
        (m) =>
          m.kind === 'PersistentVolumeClaim' &&
          m.metadata.name === 'snow-white-kafka-test-release',
      );
      expect(pvc).toBeDefined();

      return pvc;
    };

    it('should have default labels', async () => {
      const pvc = await renderAndGetPvc();

      const { metadata } = pvc;
      expect(metadata).toBeDefined();

      expectToHaveDefaultLabelsForMicroservice(metadata.labels, 'kafka');
    });

    it('should have default request size', async () => {
      const pvc = await renderAndGetPvc();

      const { spec } = pvc;
      expect(spec).toBeDefined();

      const { resources } = spec;
      expect(resources).toBeDefined();

      const { requests } = resources;
      expect(requests).toBeDefined();
      expect(requests.storage).toBe('10Gi');
    });

    it('should respect custom request size from values', async () => {
      const size = '20Gi';
      const pvc = await renderAndGetPvc({
        kafka: {
          persistence: {
            size,
          },
        },
      });

      const { spec } = pvc;
      expect(spec).toBeDefined();

      const { resources } = spec;
      expect(resources).toBeDefined();

      const { requests } = resources;
      expect(requests).toBeDefined();
      expect(requests.storage).toBe(size);
    });

    it('should have default storage class name assigned', async () => {
      const pvc = await renderAndGetPvc();

      const { spec } = pvc;
      expect(spec).toBeDefined();
      expect(spec.storageClassName).toBe('hostpath');
    });

    it('should respect custom storage class name from values', async () => {
      const storageClass = 'nfs';
      const pvc = await renderAndGetPvc({
        kafka: {
          persistence: {
            storageClass,
          },
        },
      });

      const { spec } = pvc;
      expect(spec).toBeDefined();
      expect(spec.storageClassName).toBe(storageClass);
    });
  });

  describe("Service's", () => {
    describe('headless service', () => {
      const renderAndGetKafkaHeadlessService = async (manifests?: any[]) => {
        if (!manifests) {
          manifests = await renderHelmChart({
            chartPath: 'charts/snow-white',
          });
        }

        const service = manifests.find(
          (m) =>
            m.kind === 'Service' &&
            m.metadata.name === 'snow-white-kafka-test-release',
        );
        expect(service).toBeDefined();
        return service;
      };

      it('should be Kubernetes Service', async () => {
        const service = await renderAndGetKafkaHeadlessService();

        expect(service.apiVersion).toMatch('v1');
        expect(service.kind).toMatch('Service');
        expect(service.metadata.name).toMatch('snow-white-kafka-test-release');
      });

      it('should have default labels', async () => {
        const service = await renderAndGetKafkaHeadlessService();

        const { metadata } = service;
        expect(metadata).toBeDefined();

        expectToHaveDefaultLabelsForMicroservice(metadata.labels, 'kafka');
      });

      describe('type', async () => {
        it("should be headless ('None')", async () => {
          const service = await renderAndGetKafkaHeadlessService();

          const { spec } = service;
          expect(spec).toBeDefined();

          expect(spec.clusterIP).toBe('None');
        });
      });

      describe('selector', () => {
        it('should contain immutable labels', async () => {
          const service = await renderAndGetKafkaHeadlessService();

          const { spec } = service;
          expect(spec).toBeDefined();

          expect(spec.selector).toEqual({
            'app.kubernetes.io/component': 'kafka',
            'app.kubernetes.io/instance': 'test-release',
            'app.kubernetes.io/name': 'kafka',
            'app.kubernetes.io/part-of': 'snow-white',
          });
        });

        it('should select correct pod based on selector labels', async () => {
          const manifests = await renderHelmChart({
            chartPath: 'charts/snow-white',
          });

          const service = await renderAndGetKafkaHeadlessService(manifests);

          const deployment = manifests.find(
            (m) =>
              m.kind === 'StatefulSet' &&
              isSubset(service.spec.selector, m.metadata?.labels),
          );

          expect(deployment).toBeDefined();
        });
      });
    });

    describe('Service', () => {
      const renderAndGetKafkaConnectService = async (manifests?: any[]) => {
        if (!manifests) {
          manifests = await renderHelmChart({
            chartPath: 'charts/snow-white',
          });
        }

        const service = manifests.find(
          (m) =>
            m.kind === 'Service' &&
            m.metadata.name === 'snow-white-kafka-connect-test-release',
        );
        expect(service).toBeDefined();
        return service;
      };

      it('should be Kubernetes Service', async () => {
        const service = await renderAndGetKafkaConnectService();

        expect(service.apiVersion).toMatch('v1');
        expect(service.kind).toMatch('Service');
        expect(service.metadata.name).toMatch(
          'snow-white-kafka-connect-test-release',
        );
      });

      it('should have default labels', async () => {
        const service = await renderAndGetKafkaConnectService();

        const { metadata } = service;
        expect(metadata).toBeDefined();

        expectToHaveDefaultLabelsForMicroservice(metadata.labels, 'kafka');
      });

      describe('type', async () => {
        it("should be headless 'ClusterIP'", async () => {
          const service = await renderAndGetKafkaConnectService();

          const { spec } = service;
          expect(spec).toBeDefined();

          expect(spec.type).toBe('ClusterIP');
        });
      });

      describe('selector', () => {
        it('should contain immutable labels', async () => {
          const service = await renderAndGetKafkaConnectService();

          const { spec } = service;
          expect(spec).toBeDefined();

          expect(spec.selector).toEqual({
            'app.kubernetes.io/component': 'kafka',
            'app.kubernetes.io/instance': 'test-release',
            'app.kubernetes.io/name': 'kafka',
            'app.kubernetes.io/part-of': 'snow-white',
          });
        });

        it('should select correct pod based on selector labels', async () => {
          const manifests = await renderHelmChart({
            chartPath: 'charts/snow-white',
          });

          const service = await renderAndGetKafkaConnectService(manifests);

          const deployment = manifests.find(
            (m) =>
              m.kind === 'StatefulSet' &&
              isSubset(service.spec.selector, m.metadata?.labels),
          );

          expect(deployment).toBeDefined();
        });
      });
    });
  });
});
