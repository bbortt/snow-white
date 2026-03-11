/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { renderHelmChart } from './render-helm-chart';
import { describe, it, expect } from 'vitest';
import { expectToHaveDefaultLabelsForMicroservice } from './helpers';
import { defaultLogPattern } from './constants';

const getTemplateSpec = (cronJob: any) => {
  const { spec } = cronJob;
  expect(spec).toBeDefined();

  const { jobTemplate } = spec;
  expect(jobTemplate).toBeDefined();

  const jobSpec = jobTemplate.spec;
  expect(jobSpec).toBeDefined();

  const { template } = jobSpec;
  expect(template).toBeDefined();

  const templateSpec = template.spec;
  expect(templateSpec).toBeDefined();

  return templateSpec;
};

const valuesWithEnabledApiSyncJob = {
  snowWhite: {
    apiSyncJob: {
      enabled: true,
      additionalEnvs: [
        {
          name: 'SNOW_WHITE_API_SYNC_JOB_ARTIFACTORY_ACCESS-TOKEN',
          valueFrom: {
            secretKeyRef: {
              name: 'artifactory-credentials',
              key: 'access-token',
            },
          },
        },
      ],
    },
  },
};

describe('API Sync Job', () => {
  it('should not be enabled by default', async () => {
    const manifests = await renderHelmChart({
      chartPath: 'charts/snow-white',
    });

    const apiSyncJob = manifests.find((m) => m.kind === 'CronJob');
    expect(apiSyncJob).toBeUndefined();
  });

  const renderAndGetCronJob = async (manifests?: any[]) => {
    if (!manifests) {
      manifests = await renderHelmChart({
        chartPath: 'charts/snow-white',
        values: valuesWithEnabledApiSyncJob,
      });
    }

    const apiSyncJob = manifests.find(
      (m) =>
        m.kind === 'CronJob' &&
        m.metadata.name === 'snow-white-api-sync-job-test-release',
    );
    expect(apiSyncJob).toBeDefined();

    return apiSyncJob;
  };

  it('should render if enabled and Artifactory token is present in additional envs', () => {
    expect(renderAndGetCronJob()).toBeDefined();
  });

  it('should be kubernetes CronJob', async () => {
    const cronJob = await renderAndGetCronJob();

    expect(cronJob.apiVersion).toMatch('v1');
    expect(cronJob.kind).toMatch('CronJob');

    const { metadata } = cronJob;
    expect(metadata).toBeDefined();

    expect(metadata.name).toMatch('snow-white-api-sync-job-test-release');
  });

  it('should have default labels', async () => {
    const cronJob = await renderAndGetCronJob();

    const { metadata } = cronJob;
    expect(metadata).toBeDefined();

    expectToHaveDefaultLabelsForMicroservice(metadata.labels, 'api-sync-job');
  });

  it('should truncate long release name', async () => {
    const manifests = await renderHelmChart({
      chartPath: 'charts/snow-white',
      // 53 chars is the max length for Helm release names
      releaseName: 'very-long-test-release-name-that-exceeds-the-limit',
      values: valuesWithEnabledApiSyncJob,
    });

    const cronJob = manifests.find(
      (m) =>
        m.kind === 'CronJob' &&
        m.metadata.name ===
          'snow-white-api-sync-job-very-long-test-release-name-that-exceed',
    );

    expect(cronJob).toBeDefined();
    expect(cronJob.metadata.name).toHaveLength(63);
  });

  const getJobTemplateSpec = (cronJob: any): any => {
    const { spec } = cronJob;
    expect(spec).toBeDefined();

    const { jobTemplate } = spec;
    expect(jobTemplate).toBeDefined();

    const templateSpec = jobTemplate.spec;
    expect(templateSpec).toBeDefined();
    return templateSpec;
  };

  it('should have default active deadline seconds', async () => {
    const cronJob = await renderAndGetCronJob();

    const templateSpec = getJobTemplateSpec(cronJob);
    expect(templateSpec.activeDeadlineSeconds).toBe(1800);
  });

  it('should include active deadline seconds from values', async () => {
    const activeDeadlineSeconds = 1234;

    const cronJob = await renderAndGetCronJob(
      await renderHelmChart({
        chartPath: 'charts/snow-white',
        values: {
          snowWhite: {
            apiSyncJob: {
              ...valuesWithEnabledApiSyncJob.snowWhite.apiSyncJob,
              activeDeadlineSeconds,
            },
          },
        },
      }),
    );

    const templateSpec = getJobTemplateSpec(cronJob);
    expect(templateSpec.activeDeadlineSeconds).toBe(activeDeadlineSeconds);
  });

  describe('template metadata', () => {
    const getTemplateMetadata = (cronJob: any) => {
      const { spec } = cronJob;
      expect(spec).toBeDefined();

      const { jobTemplate } = spec;
      expect(jobTemplate).toBeDefined();

      const jobSpec = jobTemplate.spec;
      expect(jobSpec).toBeDefined();

      const { template } = jobSpec;
      expect(template).toBeDefined();

      const { metadata } = template;
      expect(metadata).toBeDefined();

      return metadata;
    };

    describe('annotations', () => {
      it('should not have any annotations by default', async () => {
        const metadata = await getTemplateMetadata(await renderAndGetCronJob());

        expect(metadata.annotations).toBeNull();
      });

      it('should include optional annotations', async () => {
        const podAnnotations = {
          foo: 'bar',
        };

        const metadata = await getTemplateMetadata(
          await renderAndGetCronJob(
            await renderHelmChart({
              chartPath: 'charts/snow-white',
              values: {
                snowWhite: {
                  ...valuesWithEnabledApiSyncJob.snowWhite,
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
      const metadata = await getTemplateMetadata(await renderAndGetCronJob());

      expectToHaveDefaultLabelsForMicroservice(metadata.labels, 'api-sync-job');
    });
  });

  describe('template spec', () => {
    describe('imagePullSecrets', () => {
      it('should have none by default', async () => {
        const cronJob = await renderAndGetCronJob();

        const { spec } = cronJob;
        expect(spec).toBeDefined();

        expect(spec.imagePullSecrets).toBeUndefined();
      });

      it('renders with custom image pull secret based on values', async () => {
        const token = 'something';
        const cronJob = await renderAndGetCronJob(
          await renderHelmChart({
            chartPath: 'charts/snow-white',
            values: {
              ...valuesWithEnabledApiSyncJob,
              global: {
                imagePullSecrets: {
                  token,
                },
              },
            },
          }),
        );

        const templateSpec = getTemplateSpec(cronJob);
        expect(templateSpec.imagePullSecrets).toEqual({ token });
      });
    });
  });

  describe('containers', () => {
    it('should have only one (api-sync-job)', async () => {
      const templateSpec = getTemplateSpec(await renderAndGetCronJob());

      const { containers } = templateSpec;
      expect(containers).toHaveLength(1);

      expect(containers[0].name).toBe('api-sync-job');
    });

    describe('api-sync-job', () => {
      const renderAndGetApiSyncJobContainer = async (manifests?: any[]) => {
        const templateSpec = getTemplateSpec(
          await renderAndGetCronJob(manifests),
        );

        const { containers } = templateSpec;
        expect(containers).toBeDefined();

        return containers[0];
      };

      describe('image', () => {
        it('should be pulled from ghcr.io by default', async () => {
          const apiSyncJob = await renderAndGetApiSyncJobContainer();

          expect(apiSyncJob.image).toBe(
            'ghcr.io/bbortt/snow-white/api-sync-job:v1.0.0',
          );
        });

        it('should adjust the image registry from values', async () => {
          const customRegistry = 'custom.registry';

          const apiSyncJob = await renderAndGetApiSyncJobContainer(
            await renderHelmChart({
              chartPath: 'charts/snow-white',
              values: {
                snowWhite: {
                  ...valuesWithEnabledApiSyncJob.snowWhite,
                  image: { registry: customRegistry },
                },
              },
            }),
          );

          expect(apiSyncJob.image).toBe(
            'custom.registry/bbortt/snow-white/api-sync-job:v1.0.0',
          );
        });

        it('should adjust the image tag from values', async () => {
          const customTag = 'custom.tag';

          const apiSyncJob = await renderAndGetApiSyncJobContainer(
            await renderHelmChart({
              chartPath: 'charts/snow-white',
              values: {
                snowWhite: {
                  apiSyncJob: {
                    ...valuesWithEnabledApiSyncJob.snowWhite.apiSyncJob,
                    image: { tag: customTag },
                  },
                },
              },
            }),
          );

          expect(apiSyncJob.image).toBe(
            'ghcr.io/bbortt/snow-white/api-sync-job:custom.tag',
          );
        });
      });

      describe('imagePullPolicy', () => {
        it('should pull images if they are not present by default', async () => {
          const apiSyncJob = await renderAndGetApiSyncJobContainer();

          expect(apiSyncJob.imagePullPolicy).toBe('Always');
        });

        it('should adjust the image pull policy from values', async () => {
          const imagePullPolicy = 'my.policy';

          const apiSyncJob = await renderAndGetApiSyncJobContainer(
            await renderHelmChart({
              chartPath: 'charts/snow-white',
              values: {
                ...valuesWithEnabledApiSyncJob,
                image: { pullPolicy: imagePullPolicy },
              },
            }),
          );

          expect(apiSyncJob.imagePullPolicy).toBe(imagePullPolicy);
        });
      });

      describe('env', () => {
        it('should deploy 3+1+3+1 environment variables by default', async () => {
          const apiSyncJob = await renderAndGetApiSyncJobContainer();
          expect(apiSyncJob.env).toHaveLength(8);
        });

        it('should include default log pattern', async () => {
          const apiSyncJob = await renderAndGetApiSyncJobContainer();

          const protocol = apiSyncJob.env.find(
            (env) => env.name === 'LOGGING_PATTERN_CONSOLE',
          );
          expect(protocol).toBeDefined();
          expect(protocol.value).toBe(defaultLogPattern);
        });

        it('should include configuration for the OTEL collector', async () => {
          const apiSyncJob = await renderAndGetApiSyncJobContainer();

          const protocol = apiSyncJob.env.find(
            (env) => env.name === 'OTEL_EXPORTER_OTLP_PROTOCOL',
          );
          expect(protocol).toBeDefined();
          expect(protocol.value).toBe('grpc');

          const endpoint = apiSyncJob.env.find(
            (env) => env.name === 'OTEL_EXPORTER_OTLP_ENDPOINT',
          );
          expect(endpoint).toBeDefined();
          expect(endpoint.value).toBe(
            'http://snow-white-otel-collector-test-release.default.svc.cluster.local.:4317',
          );
        });

        it('should calculate api-index-api connection string', async () => {
          const apiSyncJob = await renderAndGetApiSyncJobContainer();

          const apiSyncJobBaseUrl = apiSyncJob.env.find(
            (env) => env.name === 'SNOW_WHITE_API_SYNC_JOB_API-INDEX_BASE-URL',
          );
          expect(apiSyncJobBaseUrl).toBeDefined();

          expect(apiSyncJobBaseUrl.value).toBe(
            'http://snow-white-api-index-api-test-release.default.svc.cluster.local.:80',
          );
        });

        it('should include artifactory connection information from values', async () => {
          const baseUrl = 'http://localhost:8092/artifactory';
          const repository = 'snow-white-generic-local';

          const apiSyncJob = await renderAndGetApiSyncJobContainer(
            await renderHelmChart({
              chartPath: 'charts/snow-white',
              values: {
                snowWhite: {
                  apiSyncJob: {
                    ...valuesWithEnabledApiSyncJob.snowWhite.apiSyncJob,
                    artifactory: {
                      baseUrl,
                      repository,
                    },
                  },
                },
              },
            }),
          );

          const artifactoryBaseUrl = apiSyncJob.env.find(
            (env) =>
              env.name === 'SNOW_WHITE_API_SYNC_JOB_ARTIFACTORY_BASE-URL',
          );
          expect(artifactoryBaseUrl).toBeDefined();
          expect(artifactoryBaseUrl.value).toBe(baseUrl);

          const artifactoryRepository = apiSyncJob.env.find(
            (env) =>
              env.name === 'SNOW_WHITE_API_SYNC_JOB_ARTIFACTORY_REPOSITORY',
          );
          expect(artifactoryRepository).toBeDefined();
          expect(artifactoryRepository.value).toBe(repository);

          const artifactoryAccessToken = apiSyncJob.env.find(
            (env) =>
              env.name === 'SNOW_WHITE_API_SYNC_JOB_ARTIFACTORY_ACCESS-TOKEN',
          );
          expect(artifactoryAccessToken).toBeDefined();
          expect(artifactoryAccessToken.valueFrom.secretKeyRef).toBeDefined();
          expect(artifactoryAccessToken.valueFrom.secretKeyRef.name).toBe(
            'artifactory-credentials',
          );
          expect(artifactoryAccessToken.valueFrom.secretKeyRef.key).toBe(
            'access-token',
          );
        });

        it.each([
          {
            artifactory: {
              customApiNameJsonPath: 'custom-api-name',
            },
            envVarName:
              'SNOW_WHITE_API_SYNC_JOB_ARTIFACTORY_CUSTOM_API_NAME_JSON_PATH',
            envVarValue: 'custom-api-name',
          },
          {
            artifactory: {
              customApiVersionJsonPath: 'custom-api-version',
            },
            envVarName:
              'SNOW_WHITE_API_SYNC_JOB_ARTIFACTORY_CUSTOM_API_VERSION_JSON_PATH',
            envVarValue: 'custom-api-version',
          },
          {
            artifactory: {
              customServiceNameJsonPath: 'custom-service-name',
            },
            envVarName:
              'SNOW_WHITE_API_SYNC_JOB_ARTIFACTORY_CUSTOM_SERVICE_NAME_JSON_PATH',
            envVarValue: 'custom-service-name',
          },
        ])(
          'should accept custom json path from values: $envVarName',
          async ({
            artifactory,
            envVarName,
            envVarValue,
          }: {
            artifactory: any;
            envVarName: string;
            envVarValue: string;
          }) => {
            const apiSyncJob = await renderAndGetApiSyncJobContainer(
              await renderHelmChart({
                chartPath: 'charts/snow-white',
                values: {
                  snowWhite: {
                    apiSyncJob: {
                      enabled: true,
                      artifactory,
                      additionalEnvs:
                        valuesWithEnabledApiSyncJob.snowWhite.apiSyncJob
                          .additionalEnvs,
                    },
                  },
                },
              }),
            );

            // 1 Logging + 2 OTEL + 1 JAVA_TOOL_OPTIONS + 4 default + 1 custom configuration
            expect(apiSyncJob.env).toHaveLength(9);

            const customEnv = apiSyncJob.env.find(
              (env) => env.name === envVarName,
            );
            expect(customEnv).toBeDefined();
            expect(customEnv.value).toBe(envVarValue);
          },
        );

        it('should accept additional environment variables', async () => {
          const additionalEnvs = [
            ...valuesWithEnabledApiSyncJob.snowWhite.apiSyncJob.additionalEnvs,
            { name: 'author', value: 'bbortt' },
            { name: 'foo', value: 'bar' },
          ];

          const apiSyncJob = await renderAndGetApiSyncJobContainer(
            await renderHelmChart({
              chartPath: 'charts/snow-white',
              values: {
                snowWhite: {
                  apiSyncJob: {
                    enabled: true,
                    additionalEnvs,
                  },
                },
              },
            }),
          );

          // 1 Logging + 2 OTEL + 1 JAVA_TOOL_OPTIONS + 4 default + 2 additional
          expect(apiSyncJob.env).toHaveLength(10);

          const authorEnv = apiSyncJob.env.find((env) => env.name === 'author');
          expect(authorEnv).toBeDefined();
          expect(authorEnv.value).toBe('bbortt');

          const fooEnv = apiSyncJob.env.find((env) => env.name === 'foo');
          expect(fooEnv).toBeDefined();
          expect(fooEnv.value).toBe('bar');
        });
      });

      describe('resources', () => {
        it('should be deployed with 1024 GB memory by default', async () => {
          const apiSyncJob = await renderAndGetApiSyncJobContainer();

          expect(apiSyncJob.resources.limits.memory).toBe('1Gi');
          expect(apiSyncJob.resources.requests.memory).toBe('1Gi');
        });

        it('should adjust memory request and limit based on values', async () => {
          const request = 'my-request';
          const limit = 'my-limit';

          const apiSyncJob = await renderAndGetApiSyncJobContainer(
            await renderHelmChart({
              chartPath: 'charts/snow-white',
              values: {
                snowWhite: {
                  apiSyncJob: {
                    ...valuesWithEnabledApiSyncJob.snowWhite.apiSyncJob,
                    resources: {
                      memory: {
                        request,
                        limit,
                      },
                    },
                  },
                },
              },
            }),
          );

          expect(apiSyncJob.resources.limits.memory).toBe(limit);
          expect(apiSyncJob.resources.requests.memory).toBe(request);
        });
      });

      describe('volumeMounts', () => {
        it('should mount temporary directory only by default', async () => {
          const apiSyncJob = await renderAndGetApiSyncJobContainer();

          expect(apiSyncJob.volumeMounts).toHaveLength(1);
        });

        it('should additionally mount jssecacerts if specified in values', async () => {
          const apiSyncJob = await renderAndGetApiSyncJobContainer(
            await renderHelmChart({
              chartPath: 'charts/snow-white',
              values: {
                ...valuesWithEnabledApiSyncJob,
                jssecacerts: {
                  key: 'key',
                  secretName: 'secretName',
                },
              },
            }),
          );

          expect(apiSyncJob.volumeMounts).toHaveLength(2);

          expect(apiSyncJob.volumeMounts[1].name).toBe('truststore');
          expect(apiSyncJob.volumeMounts[1].mountPath).toBe(
            '/opt/java/lib/security',
          );
          expect(apiSyncJob.volumeMounts[1].readOnly).toBe(true);
        });
      });
    });
  });

  describe('volumes', () => {
    const renderAndGetVolumes = async (manifests?: any[]): Promise<any[]> => {
      const templateSpec = getTemplateSpec(
        await renderAndGetCronJob(manifests),
      );

      const { volumes } = templateSpec;
      expect(volumes).toBeDefined();

      return volumes;
    };

    it('should include temporary directory only by default', async () => {
      const volumes = await renderAndGetVolumes();

      expect(volumes).toHaveLength(1);
    });

    it('should fail if secret name is defined in values, but key is not', async () => {
      await expect(
        async () =>
          await renderAndGetVolumes(
            await renderHelmChart({
              chartPath: 'charts/snow-white',
              values: {
                ...valuesWithEnabledApiSyncJob,
                jssecacerts: {
                  secretName: 'secretName',
                },
              },
            }),
          ),
      ).rejects.toThrow(
        "⚠ ERROR: You must set 'jssecacerts.key' to the secret key containing the jssecacerts value!",
      );
    });

    it('should additionally include jssecacerts if specified in values', async () => {
      const secretName = 'any-secret-name';
      const key = 'some-key';

      const volumes = await renderAndGetVolumes(
        await renderHelmChart({
          chartPath: 'charts/snow-white',
          values: {
            ...valuesWithEnabledApiSyncJob,
            jssecacerts: {
              key,
              secretName,
            },
          },
        }),
      );

      expect(volumes).toHaveLength(2);

      expect(volumes[1].name).toBe('truststore');
      expect(volumes[1].secret.items).toHaveLength(1);
      expect(volumes[1].secret.items[0].key).toBe(key);
      expect(volumes[1].secret.items[0].path).toBe('jssecacerts');
      expect(volumes[1].secret.secretName).toBe(secretName);
    });
  });
});
