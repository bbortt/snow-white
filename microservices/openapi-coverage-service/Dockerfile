FROM eclipse-temurin:21-jre-alpine

ARG BUILD_DATE
ARG PROJECT_VERSION

WORKDIR /app

LABEL org.opencontainers.image.title="snow-white OpenAPI Coverage Service" \
      org.opencontainers.image.name="openapi-coverage-service" \
      org.opencontainers.image.description="OpenAPI Coverage Service of snow-white" \
      org.opencontainers.image.url="https://github.com/bbortt/snow-white" \
      org.opencontainers.image.source="https://github.com/bbortt/snow-white/tree/main/microservices/openapi-coverage-service" \
      org.opencontainers.image.documentation="https://github.com/bbortt/snow-white#readme" \
      org.opencontainers.image.version="$PROJECT_VERSION" \
      org.opencontainers.image.created="$BUILD_DATE" \
      org.opencontainers.image.authors="Timon Borter <timon.borter@gmx.ch>" \
      org.opencontainers.image.licenses="Polyform-Small-Business-1.0.0"

COPY target/*-executable.jar app.jar
COPY target/opentelemetry-javaagent/opentelemetry-javaagent-*.jar /opt/opentelemetry-javaagent.jar

ENV JVM_ARGS="-XX:+UseContainerSupport -XX:InitialRAMPercentage=50 -XX:MaxRAMPercentage=80" \
    OTEL_SERVICE_NAME="openapi-coverage-service" \
    OTEL_SERVICE_NAMESPACE="io.github.bbortt.snow.white" \
    OTEL_SERVICE_VERSION="$PROJECT_VERSION" \
    OTEL_TRACES_EXPORTER="otlp" \
    OTEL_METRICS_EXPORTER="otlp" \
    OTEL_LOGS_EXPORTER="otlp" \
    SPRING_PROFILES_ACTIVE="prod"

ENTRYPOINT ["java", \
    "$JVM_ARGS", \
    "-javaagent:/opt/opentelemetry-javaagent.jar", \
    "-jar", \
    "app.jar" \
]
