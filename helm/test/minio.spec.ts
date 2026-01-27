import { describe, expect, it } from 'vitest';
import { renderHelmChart } from './render-helm-chart';

const getStatefulSet = (manifests: any[]) => {
  const statefulSet = manifests.find(
    (m) =>
      m.kind === 'StatefulSet' &&
      m.metadata.name.startsWith('test-release-minio'),
  );
  expect(statefulSet).toBeDefined();
  return statefulSet;
};

describe('MinIO', () => {
  describe('chart', () => {
    it('should be disabled by default', async () => {
      const manifests = await renderHelmChart({
        chartPath: 'charts/snow-white',
      });

      const statefulSet = manifests.find(
        (m) =>
          m.kind === 'StatefulSet' &&
          m.metadata.name.startsWith('test-release-minio'),
      );
      expect(statefulSet).toBeUndefined();
    });

    it('can be enabled with values', async () => {
      const manifests = await renderHelmChart({
        chartPath: 'charts/snow-white',
        values: {
          minio: {
            enabled: true,
          },
        },
      });

      const statefulSet = getStatefulSet(manifests);
      expect(statefulSet.spec.template.spec.containers[0].image).toMatch(
        /^quay\.io\/minio\/minio:.+$/,
      );
    });
  });
});
