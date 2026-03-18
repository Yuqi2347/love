#!/usr/bin/env bash
set -euo pipefail

ENV_FILE="${ENV_FILE:-../.env}"
DB_SCRIPTS_DIR="${DB_SCRIPTS_DIR:-../src/main/resources/db}"
ONLY_FILE="${ONLY_FILE:-}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"
source ./db-common.sh

ENV_PATH="$(resolve_env_file "$ENV_FILE")"
load_env_file "$ENV_PATH"
get_db_config

command -v mysql >/dev/null 2>&1 || { echo "mysql client not found in PATH"; exit 1; }
[[ -d "$DB_SCRIPTS_DIR" ]] || { echo "DB scripts dir not found: $DB_SCRIPTS_DIR"; exit 1; }

declare -a scripts
if [[ -n "$ONLY_FILE" ]]; then
  target="${DB_SCRIPTS_DIR%/}/$ONLY_FILE"
  [[ -f "$target" ]] || { echo "SQL file not found: $target"; exit 1; }
  scripts=("$target")
else
  while IFS= read -r file; do
    scripts+=("$file")
  done < <(find "$DB_SCRIPTS_DIR" -maxdepth 1 -type f -name "*.sql" | sort)
fi

if [[ "${#scripts[@]}" -eq 0 ]]; then
  echo "No SQL scripts found."
  exit 1
fi

echo "Will execute ${#scripts[@]} SQL file(s) on DB '$DB_NAME'"

export MYSQL_PWD="$DB_PASS"
trap 'unset MYSQL_PWD' EXIT

for sql in "${scripts[@]}"; do
  echo "Executing: $sql"
  mysql \
    --host="$DB_HOST" \
    --port="$DB_PORT" \
    --user="$DB_USER" \
    "$DB_NAME" < "$sql"
done

echo "Migration check completed."
