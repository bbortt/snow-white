# The harness exports the verdict under the identity its own specification declares

<!-- markdownlint-disable MD036 -->

**Title**
The harness exports the verdict under the identity its own specification declares

**Lens**: ARCH

**Status**: planned

**Description**
A conformance verdict is analysable only if the span carrying it satisfies the same correlation the
application's own spans satisfy: `resource.service.name`, `span.api.name` and `span.api.version`,
the three conditions `TempoTelemetryServiceImpl` builds from `ApiInformation`.
So the harness exports a span of its own under that triple, and it takes the triple from the
specification the generated action already hands it, not from values restated on the extension.

The document is in reach at the point of harvest.
`citrus-openapi-codegen-maven-plugin` copies it onto the test classpath and exposes it parsed as a
static `OpenApiSpecification`, and every generated action builder is constructed with that
specification alongside its own operation name.
The identity is therefore read from the same document the action was generated from, by the same
`InformationExtractor` the server-side generator uses
([ARCH-015](ARCH-015-identity-declared-in-the-specification.md)) and which this extension already
has on its classpath.

It is **one span per validated response**, a child of the test's own span, not an attribute set on
that span.
A test may call several operations, and a verdict is located by `openapi.operation.id` and
`http.response.status_code`, of which a span can carry one each; putting two responses' verdicts on
one span would make them unattributable.
The test's current span is unchanged, so [SW-047](SW-047-one-current-span-per-test.md) still holds —
a child is not a second current span.

`api.name`, `api.version` and `openapi.operation.id` ride on that span as attributes, per response.
`service.name` cannot: the query builds its condition as `resource.`, and a resource is fixed when a
tracer provider is built, so no per-response code can set it.
The harness holds one tracer provider per distinct service name it reads, built on first use for
that name and sharing the exporter the test JVM already configured; the ambient SDK is left alone,
so the test's own span keeps the test JVM's identity.
That placement is not an implementation detail.
`OtelInformationFilteringService` extracts the triple from resource and span attributes merged, so a
span-level `service.name` would pass the Kafka ingestion path and never match a Tempo query —
correct against one backend and silently inert against the other
([ARCH-001](ARCH-001-pluggable-influxdb-or-tempo-telemetry-backend.md)).

The span also has to survive the collector before it reaches either backend.
The `filter/openapi-traces` processor Snow-White ships (in `dev/otel-collector/config.yaml` and the
Helm chart's collector configmap) drops a span lacking `http.request.method`, which a verdict span
has no reason to carry.
That condition becomes "carries neither `http.request.method` nor `openapi.response.schema.valid`",
so the verdict marker itself is what admits the span; the `api.name` and `api.version` conditions
are unchanged.

What the extension takes as settings is **where to read, never what**: the three JSON paths
`ARCH-015` makes operator settings, defaulting to its defaults.
That is not redundant, because this repository already overrides one.
Three `pom.xml` files pass `apiNameAttributeKey` as `info.x-api-name` to the server generator, and
those same documents also carry an `info.title`: `v1-quality-gate-api.yml` declares
`x-api-name: quality-gate` beside `title: Quality Gate API`.
A harness reading the default path would publish `api.name` as `Quality Gate API` against an
application publishing `quality-gate` — no match, and an empty verdict set that looks exactly like a
suite nobody instrumented.
The paths are stated once per build, next to the generator that already needs them.

The SDK side is already present rather than new: `AutoConfiguredOpenTelemetrySource` autoconfigures
a real SDK as soon as any `otel.*` or `OTEL_*` setting exists, and
[SW-047](SW-047-one-current-span-per-test.md) already creates one current span per test.
A document declaring no service name publishes no verdict, and a suite configuring no exporter
publishes nothing.
Neither fails the test (`CON-013`); both surface in the report instead, as the criterion's
no-verdict failure and its explanation
([SW-048](SW-048-response-schema-conformance-criterion.md)).

This is a departure, stated plainly: under
[ARCH-013](ARCH-013-test-identity-travels-as-baggage.md) and
[ARCH-020](ARCH-020-test-identity-published-not-propagated.md) the harness publishes an identity
into a request and the _application_ exports the span.
That model cannot carry a conformance verdict, because the verdict exists only in the test JVM and
the application has no way to learn it without being told — which would make the application a
participant.
For this one attribute pair, and nothing else, the harness is an exporter.

**Rationale**
The alternative was to loosen the correlation query so a test-JVM span could match on less than the
full triple.
That is rejected: `service.name` is the join the whole product rests on
([STK-001](STK-001-correlate-specs-with-telemetry.md)), the condition is unconditionally present
because `ApiInformation.serviceName` is non-null, and widening it would widen correlation for all
fourteen existing criteria to buy one new one — with `api.name` collisions across services as the
first thing it breaks.
Matching the existing key exactly costs the harness a read of a document it is holding and costs the
query nothing.

The second alternative was a report uploaded out of band — a CLI command posting conformance results
alongside the specification.
That needs a new API surface, new storage and a new identity resolution, and it puts conformance
outside the calculator and aggregation model every other criterion lives in, so a conformance
finding would not be a finding.
Exporting a span reuses the entire ingestion path for the cost of an exporter configuration.

The third was to require the triple itself as three settings on the extension, on the grounds that a
harness cannot know a given installation's path overrides.
It can: the overrides are build configuration, and the build that generates the actions is the build
that holds them.
Restating the values would put one identity in two places and make drift between them silent, which
is the failure `ARCH-015` exists to prevent — a wrong identity does not raise, it produces an empty
verdict set indistinguishable from an uninstrumented suite.
Taking the paths and reading the values keeps exactly one extraction of the triple, which is what
that spec asks for.

The fourth was the cheapest of all: set `otel.service.name` for the whole test JVM to the service
under test, one surefire property and no code.
Rejected because it relabels every span that JVM exports, `SW-047`'s included, as the application —
widening the correlation query to spans that carry no API identity and making
[CON-015](CON-015-harness-span-never-moves-a-coverage-verdict.md) a far larger claim than
the one it states.
A provider per read service name confines the new identity to the verdict span.

A provider per service name rather than one is deliberate.
In this repository both of `quality-gate-api`'s generated APIs share a service name, but nothing
makes that true in general — a module may exercise two documents — and keying on the name read from
each document costs a map lookup.

**Verification Description**
A unit test asserts the verdict span carries, as the SDK resource's `service.name`, the value read
from the specification the action was built with, and the API name, version and operation id as span
attributes under the keys `openapi-coverage-stream` correlates on.
A test points the three paths at a differently shaped document and asserts the values are read from
there, and a test asserts a document missing the service name exports no verdict span while the test
still runs to its own verdict.
A test asserts the ambient SDK's resource is untouched, so the test's own span keeps the test JVM's
identity.
A test with two specifications declaring different service names in one JVM asserts each verdict
span carries its own.
An application test in this repository, or in an example, asserts the exported span is returned by
the same correlation query the application's spans are returned by.
A test asserts the publication path reads no specification the action did not supply — no file or
classpath lookup of its own.

## Relations

**Related**

- [ARCH-023](ARCH-023-conformance-verdict-is-harvested-not-recomputed.md) — the verdict this
  export carries
- [CON-015](CON-015-harness-span-never-moves-a-coverage-verdict.md) — the invariant that
  bounds what else this new span is allowed to affect
- [ARCH-013](ARCH-013-test-identity-travels-as-baggage.md) — the publication model this departs from
- [ARCH-020](ARCH-020-test-identity-published-not-propagated.md) — the same boundary from the
  harness side, and the one exception this makes to it
- [ARCH-015](ARCH-015-identity-declared-in-the-specification.md) — the declared identity this reads,
  at the paths it makes settable
- [ARCH-001](ARCH-001-pluggable-influxdb-or-tempo-telemetry-backend.md) — the two backends the
  resource-level placement has to satisfy at once
- [STK-001](STK-001-correlate-specs-with-telemetry.md) — the correlation the triple is the join for
- [SW-047](SW-047-one-current-span-per-test.md) — the span that already exists per test
- [CON-013](CON-013-extension-never-changes-a-test-verdict.md) — why an unreachable exporter is not
  a test failure
- [SW-030](SW-030-unjudged-target-is-not-applicable.md) — what a response without a verdict
  become
