FROM openjdk:17-slim-bullseye
LABEL maintainer="Timon Borter <bbortt.github.io>"

ENTRYPOINT ["java", "-jar", "snow-white.jar"]

EXPOSE 8080
EXPOSE 9090

RUN useradd -ms /bin/sh snow-white && \
    apt update && \
    apt full-upgrade -y && \
    apt clean

USER snow-white
WORKDIR /home/snow-white

COPY build/libs/snow-white-*.jar snow-white.jar
