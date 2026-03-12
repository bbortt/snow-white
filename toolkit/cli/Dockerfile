FROM scratch

ARG BUILD_DATE
ARG PROJECT_VERSION

LABEL org.opencontainers.image.title="snow-white CLI" \
      org.opencontainers.image.name="cli" \
      org.opencontainers.image.description="CLI distribution of snow-white" \
      org.opencontainers.image.url="https://github.com/bbortt/snow-white" \
      org.opencontainers.image.source="https://github.com/bbortt/snow-white/tree/main/toolkit/cli" \
      org.opencontainers.image.documentation="https://github.com/bbortt/snow-white#readme" \
      org.opencontainers.image.version="$PROJECT_VERSION" \
      org.opencontainers.image.created="$BUILD_DATE" \
      org.opencontainers.image.authors="Timon Borter <timon.borter@gmx.ch>" \
      org.opencontainers.image.licenses="Polyform-Small-Business-1.0.0"

COPY target/snow-white-linux-arm64 /
COPY target/snow-white-linux-x64 /
COPY target/snow-white-macos-arm64 /
COPY target/snow-white-macos-x64 /
COPY target/snow-white-windows-x64.exe /
