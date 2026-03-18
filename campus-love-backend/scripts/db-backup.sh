#!/usr/bin/env bash
set -euo pipefail

ENV_FILE="${1:-../.env}"
OUTPUT_DIR="${2:-../backups/db}"
BACKUP_TYPE="${3:-full}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"
source ./db-common.sh

ENV_PATH="$(resolve_env_file "$ENV_FILE")"
load_env_file "$ENV_PATH"
get_db_config

command -v mysqldump >/dev/null 2>&1 || { echo "mysqldump not found in PATH"; exit 1; }

mkdir -p "$OUTPUT_DIR"
TIMESTAMP="$(date +%Y%m%d_%H%M%S)"
BACKUP_FILE="${OUTPUT_DIR}/${DB_NAME}_${BACKUP_TYPE}_${TIMESTAMP}.sql"

export MYSQL_PWD="$DB_PASS"
trap 'unset MYSQL_PWD' EXIT

if [[ "$BACKUP_TYPE" == "full" ]]; then
  mysqldump \
    --host="$DB_HOST" \
    --port="$DB_PORT" \
    --user="$DB_USER" \
    --single-transaction \
    --routines \
    --triggers \
    --events \
    --set-gtid-purged=OFF \
    "$DB_NAME" > "$BACKUP_FILE"
else
  echo "Unsupported BACKUP_TYPE: $BACKUP_TYPE (use: full)"
  exit 1
fi

if [[ ! -f "$BACKUP_FILE" ]]; then
  echo "Backup failed: file not created."
  exit 1
fi

SIZE="$(wc -c < "$BACKUP_FILE" | tr -d ' ')"
if [[ "$SIZE" -lt 1024 ]]; then
  echo "Backup may be invalid: file too small (${SIZE} bytes)."
  exit 1
fi

echo "Backup created: $BACKUP_FILE"
echo "File size: ${SIZE} bytes"
