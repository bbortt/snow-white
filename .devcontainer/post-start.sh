#!/usr/bin/env bash

#
# Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
# Licensed under the Polyform Small Business License 1.0.0
# See LICENSE file for full details.
#

set -euo pipefail

if docker info > /dev/null 2>&1; then
  exit 0
fi

# The docker-in-docker feature prefers the legacy iptables backend. Kernels
# compiled for nftables only - Fedora, Arch, Raspberry Pi OS, some WSL2 kernels -
# have no legacy `nat` table, and dockerd exits with "can't initialize iptables
# table `nat'". Detect that the way the feature does, by looking for the module
# rather than probing: `iptables-legacy -L` would auto-modprobe `ip_tables`.
if ! grep -qE '^ip_tables\b' /proc/modules && [ ! -d /sys/module/ip_tables ]; then
  printf '\n==> No legacy iptables support in this kernel, switching to the nft backend\n'
  sudo update-alternatives --set iptables /usr/sbin/iptables-nft
  sudo update-alternatives --set ip6tables /usr/sbin/ip6tables-nft
fi

printf '\n==> Starting the Docker daemon\n'
sudo /usr/local/share/docker-init.sh

if ! docker info > /dev/null 2>&1; then
  printf '\n!!! The Docker daemon did not start. Testcontainers-based tests and `docker compose` will not work.\n'
  printf '!!! See DEVELOPMENT.md#troubleshooting.\n'
fi
