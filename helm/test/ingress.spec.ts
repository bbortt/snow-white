import { renderHelmChart } from './render-helm-chart';
import {
  expectFailsWithMessageContaining,
  expectToHaveDefaultLabelsForMicroservice,
} from './helpers';

describe('Ingress', () => {
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

    expectToHaveDefaultLabelsForMicroservice(metadata.labels, 'ingress');
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

  describe('tls', () => {
    it('should be enabled by default', async () => {
      const ingress = await renderAndGetIngress();

      const { spec } = ingress;
      expect(spec).toBeDefined();

      expect(spec.tls).toHaveLength(1);
      const tls = spec.tls[0];
      expect(tls.hosts).toHaveLength(1);
      expect(tls.hosts[0]).toBe('localhost');
    });

    it('can be disabled with values', async () => {
      const ingress = await renderAndGetIngress(
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

      const { spec } = ingress;
      expect(spec).toBeDefined();

      expect(spec.tls).toBeUndefined();
    });
  });

  describe('host mappings', () => {
    const expectPathToMapToApiGatewayService = (apiGatewayPath): void => {
      expect(apiGatewayPath.path).toBe('/');
      expect(apiGatewayPath.pathType).toBe('Prefix');
      expect(apiGatewayPath.backend.service.name).toBe(
        'snow-white-api-gateway-test-release',
      );
      expect(apiGatewayPath.backend.service.port.name).toBe('http');
    };

    it('should include otel-collector port 4318 mapping by default', async () => {
      const ingress = await renderAndGetIngress();

      const { spec } = ingress;
      expect(spec).toBeDefined();

      expect(spec.rules).toHaveLength(1);
      const rule = spec.rules[0];
      expect(rule.host).toBe('localhost');

      expect(rule.http.paths).toHaveLength(2);
      const otelCollectorPath = rule.http.paths[0];
      expect(otelCollectorPath.path).toBe('/v1/traces');
      expect(otelCollectorPath.pathType).toBe('Prefix');
      expect(otelCollectorPath.backend.service.name).toBe(
        'snow-white-otel-collector-test-release',
      );
      expect(otelCollectorPath.backend.service.port.name).toBe('http');
    });

    it('should disable otel-collector port mapping with properties', async () => {
      const ingress = await renderAndGetIngress(
        await renderHelmChart({
          chartPath: 'charts/snow-white',
          values: {
            otelCollector: {
              exposeThroughIngress: false,
            },
          },
        }),
      );

      const { spec } = ingress;
      expect(spec).toBeDefined();

      expect(spec.rules).toHaveLength(1);
      const rule = spec.rules[0];
      expect(rule.host).toBe('localhost');

      expect(rule.http.paths).toHaveLength(1);
      const apiGatewayPath = rule.http.paths[0];
      expectPathToMapToApiGatewayService(apiGatewayPath);
    });

    it('should map to api-gateway port 80 by default', async () => {
      const ingress = await renderAndGetIngress();

      const { spec } = ingress;
      expect(spec).toBeDefined();

      expect(spec.rules).toHaveLength(1);
      const rule = spec.rules[0];
      expect(rule.host).toBe('localhost');

      expect(rule.http.paths).toHaveLength(2);
      const apiGatewayPath = rule.http.paths[1];
      expectPathToMapToApiGatewayService(apiGatewayPath);
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
      await expectFailsWithMessageContaining(
        async () =>
          await renderHelmChart({
            chartPath: 'charts/snow-white',
            withDefaultValues: false,
          }),
        "⚠ ERROR: You must set 'snowWhite.ingress.host' to the public URL!",
      );
    });
  });
});
