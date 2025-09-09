FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY target/*-executable.jar app.jar
COPY target/opentelemetry-javaagent/opentelemetry-javaagent-*.jar /opt/opentelemetry-javaagent.jar

ENV OTEL_SERVICE_NAME="report-coordination-service"
ENV OTEL_SERVICE_NAMESPACE="io.github.bbortt.snow.white"
ENV SPRING_PROFILES_ACTIVE="prod"

ENTRYPOINT ["java", \
    "-javaagent:/opt/opentelemetry-javaagent.jar", \
    "-jar", \
    "app.jar" \
]
