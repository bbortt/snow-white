FROM openjdk:17-slim-bullseye
LABEL maintainer="Timon Borter <bbortt.github.io>"

ENTRYPOINT ["java", "-jar", "snow-white.jar"]

EXPOSE 8080 # Default HTTP Port
EXPOSE 8090 # Default Spring Actuator Port
EXPOSE 9090 # Default gRPC Port

RUN useradd -ms /bin/sh snow-white && \
    apt update && \
    apt full-upgrade -y && \
    apt clean

USER snow-white
WORKDIR /home/snow-white

COPY build/libs/snow-white-*.jar snow-white.jar
