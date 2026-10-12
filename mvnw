#!/bin/sh
# ----------------------------------------------------------------------------
# Maven Wrapper Executable for Linux / macOS / CI
# ----------------------------------------------------------------------------

set -e

DIR="$(cd "$(dirname "$0")" && pwd)"

# Check if system mvn exists
if command -v mvn >/dev/null 2>&1; then
    exec mvn "$@"
fi

# Fallback: Check standard M2_HOME / MAVEN_HOME
if [ -n "$MAVEN_HOME" ] && [ -x "$MAVEN_HOME/bin/mvn" ]; then
    exec "$MAVEN_HOME/bin/mvn" "$@"
fi

if [ -n "$M2_HOME" ] && [ -x "$M2_HOME/bin/mvn" ]; then
    exec "$M2_HOME/bin/mvn" "$@"
fi

echo "Error: Maven is not installed or not in PATH." >&2
exit 1
