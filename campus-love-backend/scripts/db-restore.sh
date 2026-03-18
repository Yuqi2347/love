#!/usr/bin/env bash
set -euo pipefail

ENV_FILE="${ENV_FILE:-../.env}"
BACKUP_FILE="${1:-}"
FORCE="${FORCE:-false}"

if [[ -z "$BACKUP_FILE" ]]; then
  echo "Usage: FORCE=true ./scripts/db-restore.sh <backup_file>  (or interactive without FORCE)"
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"
source ./db-common.sh

ENV_PATH="$(resolve_env_file "$ENV_FILE")"
load_env_file "$ENV_PATH"
get_db_config

command -v mysql >/dev/null 2>&1 || { echo "mysql client not found in PATH"; exit 1; }

if [[ ! -f "$BACKUP_FILE" ]]; then
  echo "Backup file not found: $BACKUP_FILE"
  exit 1
fi

if [[ "$FORCE" != "true" ]]; then
  echo "About to restore database '$DB_NAME' from: $BACKUP_FILE"
  read -r -p "Type YES to continue: " confirm
  if [[ "$confirm" != "YES" ]]; then
    echo "Restore canceled."
    exit 0
  fi
fi

export MYSQL_PWD="$DB_PASS"
trap 'unset MYSQL_PWD' EXIT

mysql \
  --host="$DB_HOST" \
  --port="$DB_PORT" \
  --user="$DB_USER" \
  "$DB_NAME" < "$BACKUP_FILE"

echo "Restore completed: $BACKUP_FILE"
