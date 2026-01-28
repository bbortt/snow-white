FROM eclipse-temurin:25-jdk-alpine AS jlink-builder

# Essential modules that jdeps misses for Spring Boot + OpenTelemetry
ENV ESSENTIAL_MODULES="java.desktop,java.instrument,java.logging,java.management,java.naming"

RUN apk add --no-cache binutils

WORKDIR /build

COPY target/*-executable.jar app.jar
COPY target/dependencies classpath
COPY target/opentelemetry-javaagent/opentelemetry-javaagent-*.jar /classpath/opentelemetry-javaagent.jar

RUN jdeps --ignore-missing-deps \
    --class-path 'dependencies/*' \
    --multi-release 25 \
    --print-module-deps \
    --recursive \
    app.jar > deps.info

RUN ALL_MODULES=$(echo "$(cat deps.info),$ESSENTIAL_MODULES" | tr ',' '\n' | sort -u | tr '\n' ',' | sed 's/,$//') && \
    jlink \
    --add-modules "$ALL_MODULES" \
    --strip-debug \
    --no-header-files \
    --no-man-pages \
    --output /custom-jre && \
    echo "Custom JRE created successfully" && \
    echo "JRE size: $(du -sh /custom-jre | cut -f1)"

FROM alpine:3.22.1

ARG BUILD_DATE
ARG PROJECT_VERSION

WORKDIR /app

LABEL org.opencontainers.image.title="snow-white API Sync Job" \
      org.opencontainers.image.name="api-sync-job" \
      org.opencontainers.image.description="API Sync Job of snow-white" \
      org.opencontainers.image.url="https://github.com/bbortt/snow-white" \
      org.opencontainers.image.source="https://github.com/bbortt/snow-white/tree/main/microservices/api-sync-job" \
      org.opencontainers.image.documentation="https://github.com/bbortt/snow-white#readme" \
      org.opencontainers.image.version="$PROJECT_VERSION" \
      org.opencontainers.image.created="$BUILD_DATE" \
      org.opencontainers.image.authors="Timon Borter <timon.borter@gmx.ch>" \
      org.opencontainers.image.licenses="Polyform-Small-Business-1.0.0"

RUN addgroup -S api-sync-job --gid 12345 && \
    adduser -S api-sync-job -u 10000 -G api-sync-job -H

COPY --from=jlink-builder /custom-jre /opt/java

COPY target/*-executable.jar sync-job.jar
COPY target/opentelemetry-javaagent/opentelemetry-javaagent-*.jar /opt/opentelemetry-javaagent.jar

RUN chown -R api-sync-job:api-sync-job /app /opt

ENV JAVA_HOME="/opt/java" \
    OTEL_SERVICE_NAME="api-sync-job" \
    OTEL_SERVICE_NAMESPACE="io.github.bbortt.snow.white" \
    OTEL_SERVICE_VERSION="$PROJECT_VERSION" \
    JAVA_OPTS="-XX:+UseContainerSupport \
        -XX:InitialRAMPercentage=75 \
        -XX:MaxRAMPercentage=75 \
        -XX:+UseStringDeduplication \
        -XX:+UseCompressedOops"
ENV PATH="$PATH:$JAVA_HOME/bin"

USER api-sync-job

ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS --enable-preview -javaagent:/opt/opentelemetry-javaagent.jar -jar /app/sync-job.jar" ]
