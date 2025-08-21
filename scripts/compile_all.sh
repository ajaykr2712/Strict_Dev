#!/bin/zsh
set -euo pipefail
cd "$(dirname "$0")/.."
echo "Compiling all Java files..."
# Exclude potential build/output and .git folders
find . -type f -name "*.java" -not -path "*/.git/*" -print0 | xargs -0 javac
echo "Done."
