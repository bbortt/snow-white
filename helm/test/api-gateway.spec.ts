/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { describe, it, expect } from 'vitest';
import { parseDocument } from 'yaml';
import { renderHelmChart } from './render-helm-chart';
import { isSubset } from './helpers';

describe('API Gateway', () => {
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
          m.metadata.name === 'snow-white-api-gateway-test-release',
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

      expect(metadata.name).toMatch('snow-white-api-gateway-test-release');
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
            'snow-white-api-gateway-very-long-test-release-name-that-exceeds',
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
          'app.kubernetes.io/name': 'api-gateway',
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
                    'app.kubernetes.io/name': 'api-gateway',
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
      it('should have only one (api-gateway)', async () => {
        const templateSpec = getPodSpec(await renderAndGetDeployment());

        const { containers } = templateSpec;
        expect(containers).toHaveLength(1);

        expect(containers[0].name).toBe('api-gateway');
      });

      describe('api-gateway', () => {
        const renderAndGetApiGatewayContainer = async (manifests?: any[]) => {
          const templateSpec = getPodSpec(
            await renderAndGetDeployment(manifests),
          );

          const { containers } = templateSpec;
          expect(containers).toBeDefined();

          return containers[0];
        };

        describe('image', () => {
          it('should be pulled from ghcr.io by default', async () => {
            const apiGateway = await renderAndGetApiGatewayContainer();

            expect(apiGateway.image).toBe(
              'ghcr.io/bbortt/snow-white/api-gateway:v1.0.0-ci.0',
            );
          });

          it('should adjust the container registry from values', async () => {
            const customRegistry = 'custom.registry';

            const apiGateway = await renderAndGetApiGatewayContainer(
              await renderHelmChart({
                chartPath: 'charts/snow-white',
                values: {
                  snowWhite: {
                    image: { registry: customRegistry },
                  },
                },
              }),
            );

            expect(apiGateway.image).toBe(
              'custom.registry/bbortt/snow-white/api-gateway:v1.0.0-ci.0',
            );
          });

          it('should adjust the image tag from values', async () => {
            const customTag = 'custom.tag';

            const apiGateway = await renderAndGetApiGatewayContainer(
              await renderHelmChart({
                chartPath: 'charts/snow-white',
                values: {
                  snowWhite: {
                    apiGateway: { image: { tag: customTag } },
                  },
                },
              }),
            );

            expect(apiGateway.image).toBe(
              'ghcr.io/bbortt/snow-white/api-gateway:custom.tag',
            );
          });
        });

        describe('imagePullPolicy', () => {
          it('should pull images if they are not present by default', async () => {
            const apiGateway = await renderAndGetApiGatewayContainer();

            expect(apiGateway.imagePullPolicy).toBe('IfNotPresent');
          });

          it('should adjust the image pull policy from values', async () => {
            const imagePullPolicy = 'my.policy';

            const apiGateway = await renderAndGetApiGatewayContainer(
              await renderHelmChart({
                chartPath: 'charts/snow-white',
                values: {
                  image: { pullPolicy: imagePullPolicy },
                },
              }),
            );

            expect(apiGateway.imagePullPolicy).toBe(imagePullPolicy);
          });
        });

        describe('env', () => {
          it('should deploy 3 environment variables by default', async () => {
            const apiGateway = await renderAndGetApiGatewayContainer();
            expect(apiGateway.env).toHaveLength(3);
          });

          it('should include public url from values without TLS', async () => {
            const apiGateway = await renderAndGetApiGatewayContainer(
              await renderHelmChart({
                chartPath: 'charts/snow-white',
                values: {
                  snowWhite: {
                    ingress: {
                      tls: false,
                    },
                  },
                },
              }),
            );

            const publicDomain = apiGateway.env.find(
              (env) => env.name === 'SNOW_WHITE_API_GATEWAY_PUBLIC-URL',
            );
            expect(publicDomain).toBeDefined();
            expect(publicDomain.value).toBe('http://localhost');
          });

          it('should include public url from values with TLS', async () => {
            const apiGateway = await renderAndGetApiGatewayContainer();

            const publicDomain = apiGateway.env.find(
              (env) => env.name === 'SNOW_WHITE_API_GATEWAY_PUBLIC-URL',
            );
            expect(publicDomain).toBeDefined();
            expect(publicDomain.value).toBe('https://localhost');
          });

          it('should calculate service connections based on release name', async () => {
            const apiGateway = await renderAndGetApiGatewayContainer();

            const qualityGateUrl = apiGateway.env.find(
              (env) =>
                env.name === 'SNOW_WHITE_API_GATEWAY_QUALITY-GATE-API-URL',
            );
            expect(qualityGateUrl).toBeDefined();
            expect(qualityGateUrl.value).toBe(
              'http://snow-white-quality-gate-api-test-release.default.svc.cluster.local.:8080',
            );

            const reportCoordinatorUrl = apiGateway.env.find(
              (env) =>
                env.name ===
                'SNOW_WHITE_API_GATEWAY_REPORT-COORDINATOR-API-URL',
            );
            expect(reportCoordinatorUrl).toBeDefined();
            expect(reportCoordinatorUrl.value).toBe(
              'http://snow-white-report-coordinator-api-test-release.default.svc.cluster.local.:8080',
            );
          });

          it('should accept additional environment variables', async () => {
            const additionalEnvs = {
              author: 'bbortt',
              foo: 'bar',
            };

            const apiGateway = await renderAndGetApiGatewayContainer(
              await renderHelmChart({
                chartPath: 'charts/snow-white',
                values: {
                  snowWhite: {
                    apiGateway: { additionalEnvs },
                  },
                },
              }),
            );

            // 3 default + 2 additional
            expect(apiGateway.env).toHaveLength(5);

            const authorEnv = apiGateway.env.find(
              (env) => env.name === 'author',
            );
            expect(authorEnv).toBeDefined();
            expect(authorEnv.value).toBe('bbortt');

            const fooEnv = apiGateway.env.find((env) => env.name === 'foo');
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
          m.metadata.name === 'snow-white-api-gateway-test-release',
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
            'snow-white-api-gateway-very-long-test-release-name-that-exceeds',
      );

      expect(pdb).toBeDefined();
    });

    describe('selector', () => {
      it('should contain immutable labels', async () => {
        const pdb = await renderAndGetPdb();

        const { spec } = pdb;
        expect(spec).toBeDefined();

        expect(spec.selector.matchLabels).toEqual({
          'app.kubernetes.io/instance': 'test-release',
          'app.kubernetes.io/name': 'api-gateway',
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
    const renderAndGetApiGatewayService = async (manifests?: any[]) => {
      if (!manifests) {
        manifests = await renderHelmChart({
          chartPath: 'charts/snow-white',
        });
      }

      const service = manifests.find(
        (m) =>
          m.kind === 'Service' &&
          m.metadata.name === 'snow-white-api-gateway-test-release',
      );
      expect(service).toBeDefined();
      return service;
    };

    const renderAndGetService = async () => {
      const service = await renderAndGetApiGatewayService();
      expect(service).toBeDefined();

      return service;
    };

    it('should be Kubernetes Service', async () => {
      const service = await renderAndGetService();

      expect(service.apiVersion).toMatch('v1');
      expect(service.kind).toMatch('Service');
      expect(service.metadata.name).toMatch(
        'snow-white-api-gateway-test-release',
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
            'snow-white-api-gateway-very-long-test-release-name-that-exceeds',
      );

      expect(service).toBeDefined();
      expect(service.metadata.name).toHaveLength(63);
    });

    describe('type', async () => {
      it("should be 'ClusterIP' by default", async () => {
        const service = await renderAndGetService();

        const { spec } = service;
        expect(spec).toBeDefined();

        expect(spec.type).toBe('ClusterIP');
      });

      it('should be changeable with values', async () => {
        const customType = 'custom';

        const service = await renderAndGetApiGatewayService(
          await renderHelmChart({
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
          }),
        );

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

        const service = await renderAndGetApiGatewayService(
          await renderHelmChart({
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
          }),
        );

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

        const service = await renderAndGetApiGatewayService(manifests);

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
    const renderAndGetIngress = async (manifests?: any[]) => {
      if (!manifests) {
        manifests = await renderHelmChart({
          chartPath: 'charts/snow-white',
        });
      }

      const ingress = manifests.find(
        (m) =>
          m.kind === 'Ingress' && m.metadata.name === 'snow-white-test-release',
      );
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
      it('should have none by default', async () => {
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

        const ingress = await renderAndGetIngress(
          await renderHelmChart({
            chartPath: 'charts/snow-white',
            values: {
              snowWhite: {
                ingress: {
                  enabled: true,
                  annotations,
                },
              },
            },
          }),
        );

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

        const ingress = await renderAndGetIngress(
          await renderHelmChart({
            chartPath: 'charts/snow-white',
            values: {
              snowWhite: {
                ingress: {
                  className: ingressClassName,
                },
              },
            },
          }),
        );

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
          'snow-white-api-gateway-test-release',
        );
        expect(path.backend.service.port.name).toBe('http');
      });

      it('should adjust hostname based on values', async () => {
        const hostname = 'not-localhost';

        const ingress = await renderAndGetIngress(
          await renderHelmChart({
            chartPath: 'charts/snow-white',
            values: {
              snowWhite: {
                ingress: {
                  host: hostname,
                },
              },
            },
          }),
        );

        expect(ingress.spec.tls).toHaveLength(1);
        const tls = ingress.spec.tls[0];
        expect(tls.hosts).toHaveLength(1);
        expect(tls.hosts[0]).toBe(hostname);

        expect(ingress.spec.rules).toHaveLength(1);
        const rule = ingress.spec.rules[0];
        expect(rule.host).toBe(hostname);
      });

      it('is required to specify a public host', async () => {
        expectFailsWithMessageContaining(
          async () =>
            await renderHelmChart({
              chartPath: 'charts/snow-white',
              withDefaultValues: false,
            }),
          "ERROR: You must set 'snowWhite.ingress.host' to the public URL!",
        );
      });

      const expectFailsWithMessageContaining = async (
        callback: Function,
        part: string,
      ): Promise<void> => {
        try {
          await callback();
        } catch (error) {
          expect(error.message).contains(part);

          return;
        }

        expect.fail(
          `Expected code to throw exception containing message '${part}'!`,
        );
      };
    });
  });
});
