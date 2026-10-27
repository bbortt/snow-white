# AsyncAPI

## AsyncAPI Attributes

This group describes the attributes used to correlate [Open Telemetry](https://github.com/open-telemetry) data with [AsyncAPI](https://www.asyncapi.com) specifications.

| Attribute                                                                               | Type   | Description                                                                                  | Examples                  | Stability                                                      |
| --------------------------------------------------------------------------------------- | ------ | -------------------------------------------------------------------------------------------- | ------------------------- | -------------------------------------------------------------- |
| <a id="asyncapi-name" href="#asyncapi-name">`api.name`</a>                              | String | Name of the attribute correlating an OTel span with the name of the AsyncAPI it presents.    | `Streetlights Kafka API`  | ![Development](https://img.shields.io/badge/-development-blue) |
| <a id="asyncapi-version" href="#asyncapi-version">`api.version`</a>                     | String | Name of the attribute correlating an OTel span with the version of the AsyncAPI it presents. | `1.2.3`                   | ![Development](https://img.shields.io/badge/-development-blue) |
| <a id="asyncapi-operation-id" href="#asyncapi-operation-id">`asyncapi.operation.id`</a> | String | Name of the attribute correlating an OTel span with a specific operation of an AsyncAPI.     | `receiveLightMeasurement` | ![Development](https://img.shields.io/badge/-development-blue) |

`api.name` and `api.version` are the same two attributes [`semantic-convention/openapi.md`](openapi.md) defines, carrying the same meaning: they name the document, not the format.
A correlator reading them does not need to know which kind of specification produced the span, which is what keeps the quality-gate model format-agnostic per [SYS-003](../docs/spec/specs/SYS-003-openapi-as-indexed-format.md).
Only the operation attribute is format-specific, because an AsyncAPI operation and an OpenAPI operation are addressed differently.

Snow-White defines **no** messaging attributes of its own.
The channel a message travelled on, its destination, the consumer group and the system are already covered by [OpenTelemetry's messaging semantic conventions](https://opentelemetry.io/docs/specs/semconv/messaging/messaging-spans/) and are emitted by the instrumentation, not by Snow-White.
In particular `messaging.destination.name` carries the topic, so the AsyncAPI channel needs no attribute of its own.

Message-level attributes — which of a channel's messages a payload actually matched, and whether it conformed to that message's schema — are **not emitted yet**.
They require resolving the AsyncAPI document at runtime, which the toolkit does not do today.

## Getting the attributes onto the span

Unlike `test.case.name`, these attributes are not baggage: they describe the service's own specification, so the service is the only party that knows them.
They are declared on the listener with `@SnowWhiteInformation` and copied onto the span by [`toolkit/spring-kafka-autoconfiguration`](../toolkit/spring-kafka-autoconfiguration):

```java
@SnowWhiteInformation(
  serviceName = "streetlights",
  apiName = "Streetlights Kafka API",
  apiVersion = "1.0.0"
)
@Component
class LightMeasurementListener {

  @SnowWhiteInformation(operationId = "receiveLightMeasurement")
  @KafkaListener(topics = "smartylighting.streetlights.event.lighting.measured")
  void onLightMeasured(LightMeasured event) {
    // ...
  }
}
```

The annotation resolves the same way it does for Spring Web: a value set on the method wins over the same value set on the class, so the document identity is declared once per listener class and the operation once per method.
A listener carrying an incomplete `@SnowWhiteInformation` fails the context startup rather than emitting half an identity.

The attributes are keyed by topic, resolved once at startup, which means only a single method-level `@KafkaListener` naming literal or `${...}` topics is recognised today.
Repeated `@KafkaListener` annotations, a class-level `@KafkaListener` dispatching to `@KafkaHandler` methods, `topicPattern()`, `topicPartitions()` and `#{...}` SpEL topics stamp nothing — the span is emitted as the instrumentation produced it.

### Only the receiving side

The toolkit stamps these attributes on **consumed** records only — the span an inbound message is processed under.
Nothing is stamped when the application _publishes_.

This mirrors [`toolkit/spring-web-autoconfiguration`](../toolkit/spring-web-autoconfiguration), which instruments the server side of HTTP and not outbound calls: in both cases the span describes this service answering for its own specification.
Covering the publishing side is a different question — whether a channel was _produced to_ is a property of the producer's document, not the consumer's — and it is deliberately left open.
