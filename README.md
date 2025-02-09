<h1 align="center">Snow-White 🍎</h1>

<p align="center">
    <i>An awesome pairing with, well.. do you know Snow White and the Jaeger?</i>
</p>

Snow-White makes API testing effortless by leveraging API specifications and [OpenTelemetry (OTEL)](https://opentelemetry.io/) data.
OTEL is a standardized protocol that allows flexible data sourcing.

Most commonly, Snow-White listens to **black-box tests of your application**, such as system or integration tests.
However, it can also gather insights from a **live production environment**.

Snow-White provides valuable insights on:

- Coverage
- API Performance
- And more.

## Architecture

```plantuml
@startuml

!theme vibrant

component "API Sync Job" as ApiSyncJob #Darkorange
component "Service Interface Repository" as sir
component "Redis" #Teal
component "Kafka" #Teal {
    queue "Inbound Topic" as InboundTopic
    queue "Outbound Topic" as OutboundTopic
}
component "Otel Collector" as otel #Teal
component "Kafka Event Filter" as EventFilter #Darkorange
component "InfluxDB" #Teal

ApiSyncJob --> sir : Synchronizes APIs via HTTP/S
ApiSyncJob --> Redis : Stores meta information

otel --> InboundTopic : Sends traces
InboundTopic --> EventFilter : Delivers traces
EventFilter --> Redis : Fetches meta information
EventFilter --> OutboundTopic : Sends filtered traces
OutboundTopic --> otel : Delivers filtered traces
otel --> InfluxDB : Persists filtered traces

legend right
<back:Darkorange>+</back> Snow-White
<back:Teal>+</back> Third Party
end legend

@enduml
```
