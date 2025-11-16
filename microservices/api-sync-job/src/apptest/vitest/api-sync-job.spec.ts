import type { IWireMockRequest, IWireMockResponse } from 'wiremock-captain';
import { WireMock } from 'wiremock-captain';
import { execa } from 'execa';

describe('API Sync Job', () => {
  it("should process 1'000 OpenAPI specifications in 3 minutes", async () => {
    const wiremockEndpoint =
      process.env.WIREMOCK_ENDPOINT ?? 'http://localhost:8080';
    const wiremock = new WireMock(wiremockEndpoint);

    const request: IWireMockRequest = {
      method: 'POST',
      endpoint: '/test-endpoint',
      body: {
        hello: 'world',
      },
    };
    const mockedResponse: IWireMockResponse = {
      status: 200,
      body: { goodbye: 'world' },
    };
    await wiremock.register(request, mockedResponse);

    await execa('java', [
      '-jar',
      'target/api-sync-job-1.0.0-SNAPSHOT-executable.jar',
    ]);
  });
});
