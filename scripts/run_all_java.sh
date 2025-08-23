#!/usr/bin/env bash
set -euo pipefail
root_dir="$(cd "$(dirname "$0")/.." && pwd)"
mapfile -t files < <(find "$root_dir" -type f -name '*.java')
for f in "${files[@]}"; do
  dir="$(dirname "$f")"; base="$(basename "$f")"; cls="${base%.java}"
  pushd "$dir" >/dev/null
  if javac "$base" 2>/dev/null; then
    echo "Running $f"; java "$cls" || true
  else
    echo "Compile failed for $f" >&2
  fi
  popd >/dev/null
done
