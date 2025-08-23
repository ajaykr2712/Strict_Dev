#!/usr/bin/env bash
set -euo pipefail
if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <JavaFilePath.java> [args...]"
  exit 1
fi
file="$1"; shift || true
if [[ ! -f "$file" ]]; then
  echo "File not found: $file" >&2
  exit 2
fi
dir="$(cd "$(dirname "$file")" && pwd)"
base="$(basename "$file")"
class="${base%.java}"
pushd "$dir" >/dev/null
javac "$base"
java "$class" "$@"
popd >/dev/null
