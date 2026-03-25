#!/usr/bin/env bash
# 压缩上传目录内已有动态图并补列表缩略图（与线上一致）。不修改数据库。
# 用法：
#   ./compress-uploads.sh                    # 先读 .env 中 APP_UPLOAD_PATH，空则用 ~/campus-love/uploads/
#   ./compress-uploads.sh /path/to/uploads   # 指定目录
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
# 去掉末尾 / 便于 -d 判断；Java 端 Paths 接受两种写法
TARGET="${TARGET%/}"

echo "CompressLocalUploads target: ${TARGET}/"

if [[ ! -d "$TARGET" ]]; then
  echo "" >&2
  echo "【目录不存在】${TARGET}" >&2
  echo "这不是程序坏了，而是本机还没有这个路径（或 .env 里 APP_UPLOAD_PATH 没指到真实上传目录）。" >&2
  echo "" >&2
  echo "请任选其一：" >&2
  echo "  1) 在 .env 里设置 APP_UPLOAD_PATH=后端实际存图的绝对路径（与运行中的 Spring 配置一致）" >&2
  echo "  2) 直接指定: ./compress-uploads.sh /你的/uploads绝对路径" >&2
  echo "  3) 若确认路径正确但尚未建目录: mkdir -p \"${TARGET}\"（无历史图片时跑一遍统计多为 0）" >&2
  echo "" >&2
  exit 1
fi

mvn -q compile org.codehaus.mojo:exec-maven-plugin:3.5.0:java \
  -Dexec.mainClass=com.campus.love.devtools.CompressLocalUploads \
  -Dexec.args="${TARGET}"
