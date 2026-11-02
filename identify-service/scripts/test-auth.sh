#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd -- "$script_dir/../.." && pwd)"

docker compose -f "$repo_root/docker-compose.yaml" up -d --wait pg-test-identify-service
cd "$repo_root/identify-service"
./mvnw test
