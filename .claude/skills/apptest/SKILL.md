---
name: apptest
description: Use when writing, extending, or reasoning about the Citrus black-box tests under a microservice's src/apptest directory (files matching *AppTest.java), when the user mentions "apptest", "Citrus", "black-box test", or wants a reference behavioral test suite for a service (e.g. before a rewrite). These tests run against the fully packaged, running application through real Kafka topics and HTTP endpoints — never through mocked internals.
---

# Citrus Black-Box Application Tests

Snow-White's own philosophy (`RQ-0.2` in `docs/_pages/requirements.md`) is that black-box tests
are the primary source of truth. `src/apptest` is where each microservice applies that to itself:
these tests exercise the packaged, running application exactly as production would, with real
Kafka topics and real (WireMock-stubbed) downstream HTTP calls — never mocked internals.

## Where things live

```text
microservices/<service>/
├── pom.xml                         # `apptest` Maven profile: docker-maven-plugin
│                                    # (start/stop containers) + maven-failsafe-plugin
│                                    # including **/*AppTest.java; surefire is skipped
│                                    # under this profile so only failsafe runs.
└── src/apptest/
    ├── java/.../<Service>AppTest.java
    └── resources/
        ├── docker-compose-apptest.yaml   # brings up real deps (Kafka, WireMock, ...)
        └── citrus-application.properties
```

`citrus.version` is pinned centrally in `microservices/pom.xml` (currently `5.0.0-M2`);
`citrus-junit-jupiter` + `citrus-kafka` are added as test-scope deps only inside the `apptest`
profile, so they don't leak into the default `test` phase.

To run: `./mvnw -pl :<service> -am -P apptest verify` from the repo root (requires Docker/Podman).
CI triggers apptests when a PR carries the `include:apptests` label (see `DEVELOPMENT.md`).
In
`.github/workflows/pull-requests.yml`, each matrix entry starts only its _own_
`docker-compose-apptest.yaml` (via `docker compose config --services`, filtering out
dev-convenience-only containers like `kafka.ui`) instead of a shared services block for every
matrix entry — keep `docker-compose-apptest.yaml` scoped to what that microservice's tests
actually need (see "Resource usage" in `CLAUDE.md`), and add a `healthcheck` to any new service
so `docker compose up --wait` can tell it's actually ready.

## Anatomy of an AppTest class

```java
@CitrusSupport
class SomeServiceAppTest {

  @BindToRegistry
  private final KafkaEndpoint inboundEndpoint = KafkaEndpoint.builder()
    .randomConsumerGroup(true)
    .server(
      getProperty("spring.kafka.bootstrap.servers", KAFKA_BOOTSTRAP_SERVERS)
    )
    .topic(getProperty(INBOUND_TOPIC_PROPERTY_NAME, "default-topic-name"))
    .useThreadSafeConsumer()
    .build();

  @BeforeAll
  static void beforeAllSetup() {
    WireMock.configureFor(
      getProperty("wiremock.host", "localhost"),
      parseInt(getProperty("wiremock.port", "9000"))
    );
  }

  @BeforeEach
  void beforeEachSetup() {
    reset(); // WireMock.reset()
  }

  @Test
  @CitrusTest
  void shouldDoSomething(@CitrusResource TestActionRunner runner) {
    stubFor(get(urlPathTemplate("/api/...")).willReturn(ok()));
    runner.run(
      send(endpoint).message(new KafkaMessage(payload).messageKey(key))
    );
    // assertions — see patterns below
  }
}
```

- `@CitrusSupport` on the class, `@Test @CitrusTest` on each method, `@CitrusResource
TestActionRunner runner` injected as a parameter.
- Endpoints are Java fields annotated `@BindToRegistry`, built with the fluent `KafkaEndpoint`/
  WireMock/etc. builders — properties should fall back to sane local defaults via
  `getProperty(NAME, "default")` so the class also works when run directly against `localhost`.
- Reuse `TestData` builder classes from the module's regular `src/test` tree for fixtures where
  they already exist, rather than inventing new fixture-building code duplicated across test
  layers.

## Asserting a message WAS produced (pass-through / positive case)

Poll with backoff instead of a fixed sleep — the pipeline is asynchronous:

```java
runner.run(
  send(inboundEndpoint).message(new KafkaMessage(payload).messageKey(key))
);

runner.run(
  repeatOnError()
    .index("i")
    .until("i = 10")
    .autoSleep(Duration.ofSeconds(2))
    .actions(
      receive(outboundEndpoint).selector(
        kafkaMessageFilter()
          .eventLookbackWindow(Duration.ofSeconds(20L))
          .kafkaMessageSelector(new SomeKeySelector(key))
          .build()
      )
    )
);
```

## Asserting a message was NOT produced (drop / negative case)

Don't guess at a sleep duration and check "still empty" — that's flaky and slow either way.
Instead, positively assert absence via a bounded receive wrapped in `assertException`:

```java
import static org.citrusframework.container.Assert.Builder.assertException;

import org.citrusframework.exceptions.MessageTimeoutException;

runner.run(
  send(inboundEndpoint).message(new KafkaMessage(payload).messageKey(key))
);

runner.run(
  assertException()
    .exception(MessageTimeoutException.class)
    .action(
      receive(outboundEndpoint)
        .timeout(6000L)
        .selector(
          kafkaMessageFilter()
            .kafkaMessageSelector(new SomeKeySelector(key))
            .build()
        )
    )
);
```

This times out **fast** (a few seconds) and fails loudly if a message unexpectedly _does_ show
up, instead of a plain `receive` timing out slowly and ambiguously.

## Filtering messages by key

Define a small local `record` implementing `KafkaMessageSelector<String>`, and register it once
in `@BeforeEach` via `factoryWithKafkaMessageSelector`:

```java
outboundEndpoint
  .getEndpointConfiguration()
  .getKafkaMessageSelectorFactory()
  .setCustomStrategies(
    factoryWithKafkaMessageSelector(
      (selectors) -> selectors.containsKey(MESSAGE_KEY_FILTER_KEY),
      (selectors) ->
        new KafkaMessageByKeySelector(
          (String) selectors.get(MESSAGE_KEY_FILTER_KEY)
        )
    )
  );
```

## Simulating a transient-then-recovered downstream failure

Use WireMock scenario state to test retry behavior end-to-end (not just via a mocked retry
config in a unit test):

```java
var scenarioName = "transient-failure";

stubFor(
  get(urlPathTemplate("/api/..."))
    .inScenario(scenarioName)
    .whenScenarioStateIs(Scenario.STARTED)
    .willSetStateTo("recovered")
    .willReturn(serverError())
);

stubFor(
  get(urlPathTemplate("/api/..."))
    .inScenario(scenarioName)
    .whenScenarioStateIs("recovered")
    .willReturn(ok())
);
```

## What to cover for a given service

Mirror the service's actual decision points, not just the happy path — matching, roughly, what
its unit tests already assert about the filtering/routing logic, but proven end-to-end:

- The straightforward pass-through/success case.
- Each distinct reason a message/request would be dropped or rejected (missing identifying
  data, unknown/not-found downstream resource, no content left after filtering, etc.) — verify
  the _specific_ WireMock call count where relevant (`verify(N, getRequestedFor(...))`), not
  just the end result.
- Retry-then-recover, and retry-exhausted-then-drop, for anything wrapped in `@Retryable`.

## Verifying without running the full stack

You can `test-compile` the `apptest` sources without Docker to catch API-usage mistakes quickly:

```shell
./mvnw -pl :<service> -am -P apptest test-compile -DskipTests -o
```

Actually executing the tests requires the Docker Compose stack from
`src/apptest/resources/docker-compose-apptest.yaml`, which the `apptest` profile's
`docker-maven-plugin` binding starts/stops automatically around `verify`.
