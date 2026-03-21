#!/usr/bin/env bash
# =============================================================================
# 数据库「辅助脚本」执行器（非 Flyway 主路径）
#
# 版本化结构变更统一由 Flyway 管理：启动后端时在 classpath:db/migration 下
# 按 V1 → V46 顺序执行，并写入 flyway_schema_history。
#
# 本脚本仅用于在明确场景下手工执行 db/ 根目录中的工具脚本，例如：
#   ONLY_FILE=repair.sql ./scripts/db-migrate-check.sh
#   ONLY_FILE=set_admin.sql ./scripts/db-migrate-check.sh
#
# 切勿用本脚本批量导入 schema.sql 或与 Flyway 重复执行同一 DDL。
# =============================================================================
set -euo pipefail

ENV_FILE="${ENV_FILE:-../.env}"
DB_SCRIPTS_DIR="${DB_SCRIPTS_DIR:-../src/main/resources/db}"
ONLY_FILE="${ONLY_FILE:-}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"
source ./db-common.sh

if [[ -z "$ONLY_FILE" ]]; then
  echo "未设置 ONLY_FILE。Flyway 迁移请通过启动应用完成（见 README / DEVELOPMENT_STANDARDS）。"
  echo "若需手工执行单个辅助 SQL，例如："
  echo "  ONLY_FILE=repair.sql $0"
  exit 1
fi

ENV_PATH="$(resolve_env_file "$ENV_FILE")"
load_env_file "$ENV_PATH"
get_db_config

command -v mysql >/dev/null 2>&1 || { echo "mysql client not found in PATH"; exit 1; }
[[ -d "$DB_SCRIPTS_DIR" ]] || { echo "DB scripts dir not found: $DB_SCRIPTS_DIR"; exit 1; }

target="${DB_SCRIPTS_DIR%/}/$ONLY_FILE"
[[ -f "$target" ]] || { echo "SQL file not found: $target"; exit 1; }

echo "Executing single file on DB '$DB_NAME': $target"

export MYSQL_PWD="$DB_PASS"
trap 'unset MYSQL_PWD' EXIT

mysql \
  --host="$DB_HOST" \
  --port="$DB_PORT" \
  --user="$DB_USER" \
  "$DB_NAME" < "$target"

echo "Done."
