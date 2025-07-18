FROM eclipse-temurin:21-jre-alpine

ARG BUILD_DATE
ARG PROJECT_VERSION

WORKDIR /app

LABEL org.opencontainers.image.title="snow-white API Gateway" \
      org.opencontainers.image.name="api-gateway" \
      org.opencontainers.image.description="API Gateway Service of snow-white" \
      org.opencontainers.image.url="https://github.com/bbortt/snow-white" \
      org.opencontainers.image.source="https://github.com/bbortt/snow-white/tree/main/microservices/api-gateway" \
      org.opencontainers.image.documentation="https://github.com/bbortt/snow-white#readme" \
      org.opencontainers.image.version="$PROJECT_VERSION" \
      org.opencontainers.image.created="$BUILD_DATE" \
      org.opencontainers.image.authors="Timon Borter <timon.borter@gmx.ch>" \
      org.opencontainers.image.licenses="Polyform-Small-Business-1.0.0"

COPY target/*-executable.jar app.jar
COPY target/opentelemetry-javaagent/opentelemetry-javaagent-*.jar /opt/opentelemetry-javaagent.jar

ENV OTEL_SERVICE_NAME="api-gateway"
ENV OTEL_SERVICE_NAMESPACE="io.github.bbortt.snow.white"
ENV OTEL_SERVICE_VERSION="$PROJECT_VERSION"
ENV OTEL_TRACES_EXPORTER="otlp"
ENV OTEL_METRICS_EXPORTER="otlp"
ENV OTEL_LOGS_EXPORTER="otlp"
ENV SPRING_PROFILES_ACTIVE="prod"

ENTRYPOINT ["java", \
    "-javaagent:/opt/opentelemetry-javaagent.jar", \
    "-jar", \
    "app.jar" \
]
