#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <gc.log>"
  exit 1
fi

GC_LOG="$1"
if [[ ! -f "$GC_LOG" ]]; then
  echo "GC log not found: $GC_LOG"
  exit 1
fi

YOUNG_COUNT="$(rg -c "Pause Young" "$GC_LOG" || true)"
FULL_COUNT="$(rg -c "Pause Full" "$GC_LOG" || true)"

TOTAL_PAUSE_MS="$(awk '
  /Pause (Young|Full)/ {
    if (match($0, /([0-9]+\.[0-9]+)ms/, m)) total += m[1];
    else if (match($0, /([0-9]+\.[0-9]+)s/, m)) total += (m[1] * 1000);
  }
  END { printf "%.2f", total + 0.0 }
' "$GC_LOG")"

echo "GC summary for: $GC_LOG"
echo "Young GC count : $YOUNG_COUNT"
echo "Full GC count  : $FULL_COUNT"
echo "Total pause ms : $TOTAL_PAUSE_MS"
