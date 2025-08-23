#!/usr/bin/env bash
set -euo pipefail
root_dir="$(cd "$(dirname "$0")/.." && pwd)"
find "$root_dir" -type f -name '*.class' -delete
echo "Removed all .class files"
