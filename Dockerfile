FROM openjdk:17-slim-bullseye
LABEL maintainer="Timon Borter <bbortt.github.io>"

ENTRYPOINT ["java", "-jar", "snow-white.jar"]

ENV JAVA_OPTS="-javaagent:/home/snow-white/opentelemetry-javaagent.jar" \
    OTEL_TRACES_EXPORTER=logging \
    OTEL_METRICS_EXPORTER=logging

EXPOSE 8080
EXPOSE 8090
EXPOSE 9090

RUN useradd -ms /bin/sh snow-white && \
    apt update && \
    apt full-upgrade -y && \
    apt install -y curl && \
    curl -o /home/snow-white/opentelemetry-javaagent.jar https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.20.2/opentelemetry-javaagent.jar && \
    apt remove -y curl && \
    apt clean

USER snow-white
WORKDIR /home/snow-white

COPY build/libs/snow-white-*.jar snow-white.jar
