FROM eclipse-temurin:25-jdk-alpine

WORKDIR /app

COPY target/*-executable.jar app.jar
COPY target/opentelemetry-javaagent/opentelemetry-javaagent-*.jar /opt/opentelemetry-javaagent.jar

ENV OTEL_SERVICE_NAME="example-snow-white-openapi-generator"
# Copies the `test.case.name` baggage entry a caller sends onto this service's own server span,
# which is all it takes to give a Snow-White finding the name of the test that produced it
# (see ../../semantic-convention/test.md). No application code takes part in this - and here not
# even generated code does, since the API is produced by the Snow-White OpenAPI generator.
ENV OTEL_JAVA_EXPERIMENTAL_SPAN_ATTRIBUTES_COPY_FROM_BAGGAGE_INCLUDE="test.case.name"

ENTRYPOINT ["java", \
    "-javaagent:/opt/opentelemetry-javaagent.jar", \
    "-jar", \
    "app.jar" \
]
