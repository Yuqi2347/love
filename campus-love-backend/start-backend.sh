#!/usr/bin/env bash
set -euo pipefail

ENV_FILE="${1:-.env}"
SKIP_RUN="${SKIP_RUN:-false}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

resolve_env_file() {
  local preferred="$1"
  if [[ -f "$preferred" ]]; then
    echo "$preferred"
    return 0
  fi
  if [[ "$preferred" == ".env" && -f ".env.example" ]]; then
    echo "No .env found, fallback to .env.example (recommended: copy to .env with real secrets)." >&2
    echo ".env.example"
    return 0
  fi
  echo "Env file not found: $preferred" >&2
  return 1
}

LOADED_COUNT=0

load_env_file() {
  local file="$1"
  LOADED_COUNT=0
  while IFS= read -r line || [[ -n "$line" ]]; do
    line="${line%$'\r'}"
    [[ -z "$line" ]] && continue
    [[ "$line" =~ ^[[:space:]]*# ]] && continue
    if [[ "$line" == *=* ]]; then
      local name="${line%%=*}"
      local value="${line#*=}"
      name="$(echo "$name" | xargs)"
      export "$name=$value"
      LOADED_COUNT=$((LOADED_COUNT + 1))
    fi
  done < "$file"
}

start_backend() {
  if command -v mvn >/dev/null 2>&1; then
    mvn spring-boot:run
    return 0
  fi
  if [[ -x "./mvnw" ]]; then
    ./mvnw spring-boot:run
    return 0
  fi
  if [[ -f "./mvnw" ]]; then
    chmod +x ./mvnw
    ./mvnw spring-boot:run
    return 0
  fi
  echo "Neither mvn nor mvnw found. Install Maven or add Maven Wrapper." >&2
  return 1
}

RESOLVED_ENV_FILE="$(resolve_env_file "$ENV_FILE")"
load_env_file "$RESOLVED_ENV_FILE"
echo "Loaded ${LOADED_COUNT} environment variables from ${RESOLVED_ENV_FILE}"

if [[ "$SKIP_RUN" == "true" ]]; then
  echo "Skip backend run (SKIP_RUN=true)."
  exit 0
fi

start_backend
