#!/usr/bin/env bash
set -euo pipefail

# DEPRECATION NOTICE:
# Keep for compatibility only. Prefer Kotlin function-owned spawns
# in src/main/kotlin/net/dodian/uber/game/content/npcs/spawns.

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
exec "$SCRIPT_DIR/generate_npc_spawn_groups.py"
