# `game-server/scripts`

## NPC spawn workflow status

- `generate_npc_spawn_groups.py` and `generate_npc_spawns_from_sql.sh` are deprecated compatibility tools.
- New or updated NPC spawns should be authored in Kotlin NPC function files:
  `src/main/kotlin/net/dodian/uber/game/content/npcs/spawns`.
- Generated `SpawnGroups` remains loaded as a compatibility baseline during migration.

## Performance harness scripts

- `run_luna_parity_perf_capture.sh` builds the game-server jar and runs a timed capture with GC logs + JFR.
- `summarize_gc_log.sh` prints a quick GC count/pause summary from a captured `gc.log`.
