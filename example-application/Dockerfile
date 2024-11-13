# Use a specific version of Java
FROM eclipse-temurin:21-jre-alpine

# Set OpenTelemetry agent version
ENV OTEL_AGENT_VERSION="2.9.0"

# Download the OpenTelemetry Java agent
RUN wget -O /opt/opentelemetry-javaagent.jar \
    https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_AGENT_VERSION}/opentelemetry-javaagent.jar

# Create app directory
WORKDIR /app

# Copy the JAR file (assuming it's named app.jar)
COPY target/*.jar app.jar

# Set OpenTelemetry service name and other configurations
ENV OTEL_SERVICE_NAME="example-application"
ENV OTEL_TRACES_EXPORTER="otlp"
ENV OTEL_METRICS_EXPORTER="otlp"
ENV OTEL_LOGS_EXPORTER="otlp"

# Run the application with OpenTelemetry agent
ENTRYPOINT ["java", \
    "-javaagent:/opt/opentelemetry-javaagent.jar", \
    "-jar", \
    "app.jar" \
]
