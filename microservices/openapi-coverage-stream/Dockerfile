FROM eclipse-temurin:25-jdk-alpine AS jlink-builder

# Essential modules that jdeps misses for Spring Boot + OpenTelemetry
ENV ESSENTIAL_MODULES="java.compiler,java.desktop,java.instrument,java.logging,java.management,java.security.sasl,java.sql"

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

LABEL org.opencontainers.image.title="snow-white OpenAPI Coverage Stream" \
      org.opencontainers.image.name="openapi-coverage-stream" \
      org.opencontainers.image.description="OpenAPI Coverage Stream of snow-white" \
      org.opencontainers.image.url="https://github.com/bbortt/snow-white" \
      org.opencontainers.image.source="https://github.com/bbortt/snow-white/tree/main/microservices/openapi-coverage-stream" \
      org.opencontainers.image.documentation="https://github.com/bbortt/snow-white#readme" \
      org.opencontainers.image.version="$PROJECT_VERSION" \
      org.opencontainers.image.created="$BUILD_DATE" \
      org.opencontainers.image.authors="Timon Borter <timon.borter@gmx.ch>" \
      org.opencontainers.image.licenses="Polyform-Small-Business-1.0.0"

RUN apk add --no-cache \
    gcompat \
    libstdc++ \
    tzdata \
    && addgroup -S openapi-coverage-stream \
    && adduser -S openapi-coverage-stream -G openapi-coverage-stream

COPY --from=jlink-builder /custom-jre /opt/java

COPY target/*-executable.jar /tmp/app.jar
COPY target/opentelemetry-javaagent/opentelemetry-javaagent-*.jar /opt/opentelemetry-javaagent.jar

ENV JAVA_HOME="/opt/java" \
    AOT_CACHE="/app/api-gateway.aot" \
    OTEL_SERVICE_NAME="openapi-coverage-stream" \
    OTEL_SERVICE_NAMESPACE="io.github.bbortt.snow.white" \
    OTEL_SERVICE_VERSION="$PROJECT_VERSION" \
    JAVA_OPTS="-XX:+UseContainerSupport \
        -XX:InitialRAMPercentage=75 \
        -XX:MaxRAMPercentage=75 \
        -XX:+UseStringDeduplication \
        -XX:+UseCompressedOops"
ENV PATH="$PATH:$JAVA_HOME/bin"

# Build AOT cache in "dev" profile to avoid configuration errors
# https://docs.spring.io/spring-boot/4.0/reference/packaging/aot-cache.html#packaging.aot-cache.aot-cache

ENV SPRING_PROFILES_ACTIVE="dev" \
    OTEL_JAVAAGENT_ENABLED="false" \
    INFLUXDB_TOKEN="influxdb-token" \
    SNOW_WHITE_OPENAPI_COVERAGE_STREAM_INIT-TOPICS="FALSE"

RUN java -Djarmode=tools -jar /tmp/app.jar extract --destination /app && \
    cd /app && \
    mv app.jar openapi-coverage-stream.jar && \
    java -XX:AOTCacheOutput=$AOT_CACHE $JAVA_OPTS -javaagent:/opt/opentelemetry-javaagent.jar -Dspring.context.exit=onRefresh -jar openapi-coverage-stream.jar && \
    rm /tmp/app.jar && \
    chown -R openapi-coverage-stream:openapi-coverage-stream /app /opt/java

# Reversed AOT cache configuration / productive Spring / OTEL configuration
ENV SPRING_PROFILES_ACTIVE="prod" \
    OTEL_JAVAAGENT_ENABLED="true" \
    INFLUXDB_TOKEN=""

USER openapi-coverage-stream

ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -XX:AOTCache=$AOT_CACHE -javaagent:/opt/opentelemetry-javaagent.jar -jar /app/openapi-coverage-stream.jar" ]
