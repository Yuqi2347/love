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

HTTP_PORT="${SERVER_PORT:-8082}"
if command -v ss >/dev/null 2>&1; then
  if ss -tlnp 2>/dev/null | grep -q ":${HTTP_PORT} "; then
    echo "提示：端口 ${HTTP_PORT} 已有进程在监听（见下）。多数情况是后端已经在运行，不必再执行本脚本。" >&2
    ss -tlnp 2>/dev/null | grep ":${HTTP_PORT} " >&2 || true
    echo "" >&2
    echo "若要重启后端，请先结束占用进程，例如：kill <上表中的 java PID>，或：fuser -k ${HTTP_PORT}/tcp" >&2
    echo "然后再运行: ./start-backend.sh" >&2
    echo "" >&2
    echo "（若已确认端口空闲但启动仍失败，再查 MySQL/Redis 与 .env 里 DB_URL、REDIS_HOST。）" >&2
    exit 1
  fi
fi

if [[ "$SKIP_RUN" == "true" ]]; then
  echo "Skip backend run (SKIP_RUN=true)."
  exit 0
fi

start_backend
