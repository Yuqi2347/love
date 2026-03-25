#!/usr/bin/env bash
# 将 uploads/ 根目录平铺的历史文件迁入 uploads/{userId}/ 并更新数据库 URL。
# 运行前请停后端、备份库与 uploads。依赖 .env 中 DB_URL、DB_USERNAME、DB_PASSWORD、APP_UPLOAD_PATH。
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

load_env_file() {
  local file="$1"
  [[ -f "$file" ]] || return 0
  while IFS= read -r line || [[ -n "$line" ]]; do
    line="${line%$'\r'}"
    [[ -z "$line" ]] && continue
    [[ "$line" =~ ^[[:space:]]*# ]] && continue
    if [[ "$line" == *=* ]]; then
      local name="${line%%=*}"
      local value="${line#*=}"
      name="$(echo "$name" | xargs)"
      export "$name=$value"
    fi
  done < "$file"
}

load_env_file ".env"

TARGET="${1:-}"
if [[ -z "${TARGET}" ]]; then
  if [[ -n "${APP_UPLOAD_PATH:-}" ]]; then
    TARGET="${APP_UPLOAD_PATH}"
  else
    TARGET="${HOME}/campus-love/uploads"
  fi
fi
TARGET="${TARGET%/}"

echo "MigrateFlatUploadsToUserDirs: ${TARGET}/ (DB from .env)"
# 必须显式指定 mainClass（勿依赖 pom 默认），否则会误跑 CompressLocalUploads
mvn -q compile org.codehaus.mojo:exec-maven-plugin:3.5.0:java \
  -Dexec.mainClass=com.campus.love.devtools.MigrateFlatUploadsToUserDirs \
  -Dexec.args="${TARGET}"
