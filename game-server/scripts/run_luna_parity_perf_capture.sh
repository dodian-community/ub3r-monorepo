#!/usr/bin/env bash
set -euo pipefail

if [[ $# -gt 1 ]]; then
  echo "Usage: $0 [duration_seconds]"
  exit 1
fi

DURATION_SECONDS="${1:-600}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
APP_DIR="$ROOT_DIR/game-server"
BUILD_DIR="$APP_DIR/build/libs"

if ! [[ "$DURATION_SECONDS" =~ ^[0-9]+$ ]] || [[ "$DURATION_SECONDS" -le 0 ]]; then
  echo "duration_seconds must be a positive integer"
  exit 1
fi

STAMP="$(date +%Y%m%d-%H%M%S)"
OUT_DIR="$APP_DIR/build/perf/luna-parity/$STAMP"
mkdir -p "$OUT_DIR"

echo "[luna-perf] Building game-server jar..."
"$ROOT_DIR/gradlew" :game-server:jar >/dev/null

JAR_PATH="$(ls -1t "$BUILD_DIR"/*.jar | head -n1)"
if [[ -z "${JAR_PATH:-}" ]]; then
  echo "No jar found under $BUILD_DIR"
  exit 1
fi

LEAK_LEVEL="${NETTY_LEAK_LEVEL:-paranoid}"
GC_LOG="$OUT_DIR/gc.log"
JFR_FILE="$OUT_DIR/capture.jfr"
SERVER_LOG="$OUT_DIR/server.log"

echo "[luna-perf] Starting timed capture for ${DURATION_SECONDS}s"
echo "[luna-perf] Output directory: $OUT_DIR"
echo "[luna-perf] Netty leak detection: $LEAK_LEVEL"

JAVA_OPTS=(
  "-Xms1G"
  "-Xmx1G"
  "-Dio.netty.leakDetection.level=${LEAK_LEVEL}"
  "-Xlog:gc*:file=${GC_LOG}:time,uptime,level,tags"
  "-XX:StartFlightRecording=filename=${JFR_FILE},settings=profile,dumponexit=true"
)

set +e
timeout "$DURATION_SECONDS" java "${JAVA_OPTS[@]}" -jar "$JAR_PATH" >"$SERVER_LOG" 2>&1
EXIT_CODE=$?
set -e

if [[ $EXIT_CODE -ne 0 && $EXIT_CODE -ne 124 ]]; then
  echo "[luna-perf] Server exited early with status $EXIT_CODE"
  exit $EXIT_CODE
fi

echo "[luna-perf] Capture complete."
echo "[luna-perf] gc.log: $GC_LOG"
echo "[luna-perf] capture.jfr: $JFR_FILE"
echo "[luna-perf] server.log: $SERVER_LOG"
