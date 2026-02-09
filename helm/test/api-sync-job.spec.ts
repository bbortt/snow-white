import { renderHelmChart } from './render-helm-chart';
import { describe, it, expect } from 'vitest';
import {
  expectToHaveDefaultLabelsForMicroservice,
  getPodSpec,
} from './helpers';

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

  it('should require Artifactory token if enabled', async () => {
    await expect(() =>
      renderHelmChart({
        chartPath: 'charts/snow-white',
        values: {
          snowWhite: {
            apiSyncJob: {
              enabled: true,
            },
          },
        },
      }),
    ).rejects.toThrow(
      "Required environment variable 'SNOW_WHITE_API_SYNC_JOB_ARTIFACTORY_ACCESS-TOKEN' is missing in snowWhite.apiSyncJob.additionalEnvs",
    );
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
            global: {
              imagePullSecrets: {
                token,
              },
            },
            ...valuesWithEnabledApiSyncJob,
          },
        }),
      );

      const templateSpec = getTemplateSpec(cronJob);
      expect(templateSpec.imagePullSecrets).toEqual({ token });
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
            'ghcr.io/bbortt/snow-white/api-sync-job:v1.0.0-ci.0',
          );
        });

        it('should adjust the image registry from values', async () => {
          const customRegistry = 'custom.registry';

          const apiSyncJob = await renderAndGetApiSyncJobContainer(
            await renderHelmChart({
              chartPath: 'charts/snow-white',
              values: {
                snowWhite: {
                  image: { registry: customRegistry },
                  ...valuesWithEnabledApiSyncJob.snowWhite,
                },
              },
            }),
          );

          expect(apiSyncJob.image).toBe(
            'custom.registry/bbortt/snow-white/api-sync-job:v1.0.0-ci.0',
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
                    image: { tag: customTag },
                    ...valuesWithEnabledApiSyncJob.snowWhite.apiSyncJob,
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
                image: { pullPolicy: imagePullPolicy },
                ...valuesWithEnabledApiSyncJob,
              },
            }),
          );

          expect(apiSyncJob.imagePullPolicy).toBe(imagePullPolicy);
        });
      });

      describe('env', () => {
        it('should deploy 2+1+3+1 environment variables by default', async () => {
          const apiSyncJob = await renderAndGetApiSyncJobContainer();
          expect(apiSyncJob.env).toHaveLength(7);
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
            'http://snow-white-api-index-api-test-release.default.svc.cluster.local.:9092',
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
                    artifactory: {
                      baseUrl,
                      repository,
                    },
                    ...valuesWithEnabledApiSyncJob.snowWhite.apiSyncJob,
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

          // 2 OTEL + 1 JAVA_TOOL_OPTIONS + 4 default + 2 additional
          expect(apiSyncJob.env).toHaveLength(9);

          const authorEnv = apiSyncJob.env.find((env) => env.name === 'author');
          expect(authorEnv).toBeDefined();
          expect(authorEnv.value).toBe('bbortt');

          const fooEnv = apiSyncJob.env.find((env) => env.name === 'foo');
          expect(fooEnv).toBeDefined();
          expect(fooEnv.value).toBe('bar');
        });
      });
    });
  });
});
