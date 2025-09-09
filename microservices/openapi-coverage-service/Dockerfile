FROM eclipse-temurin:21-jdk-alpine AS jlink-builder

# Essential modules that jdeps misses for Spring Boot + OpenTelemetry
ENV ESSENTIAL_MODULES="java.desktop,java.instrument,java.logging,java.management,java.security.sasl,java.sql"

RUN apk add --no-cache binutils

WORKDIR /build

COPY target/*-executable.jar app.jar
COPY target/dependencies classpath
COPY target/opentelemetry-javaagent/opentelemetry-javaagent-*.jar /classpath/opentelemetry-javaagent.jar

RUN jdeps --ignore-missing-deps \
    --class-path 'dependencies/*' \
    --multi-release 21 \
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

RUN apk add --no-cache \
    gcompat \
    libstdc++ \
    tzdata \
    && addgroup -S openapi-coverage-service \
    && adduser -S openapi-coverage-service -G openapi-coverage-service

COPY --from=jlink-builder /custom-jre /opt/java

COPY target/*-executable.jar app.jar
COPY target/opentelemetry-javaagent/opentelemetry-javaagent-*.jar /opt/opentelemetry-javaagent.jar

RUN chown -R openapi-coverage-service:openapi-coverage-service /app /opt/java

ENV JAVA_HOME="/opt/java" \
    OTEL_SERVICE_NAME="openapi-coverage-service" \
    OTEL_SERVICE_NAMESPACE="io.github.bbortt.snow.white" \
    OTEL_SERVICE_VERSION="$PROJECT_VERSION" \
    SPRING_PROFILES_ACTIVE="prod" \
    JAVA_OPTS="-XX:+UseContainerSupport \
        -XX:+TieredCompilation \
        -XX:TieredStopAtLevel=1 \
        -Xss256k \
        -XX:+UseG1GC \
        -XX:MaxGCPauseMillis=100 \
        -XX:+UseStringDeduplication \
        -XX:+UseCompressedOops"
ENV PATH="$PATH:$JAVA_HOME/bin"

USER openapi-coverage-service

ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -javaagent:/opt/opentelemetry-javaagent.jar -jar app.jar" ]
