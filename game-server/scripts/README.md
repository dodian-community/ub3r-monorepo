# `game-server/scripts`

## NPC spawn workflow status

- `generate_npc_spawn_groups.py` and `generate_npc_spawns_from_sql.sh` are deprecated compatibility tools.
- New or updated NPC spawns should be authored in Kotlin NPC function files:
  `src/main/kotlin/net/dodian/uber/game/content/npcs/spawns`.
- Generated `SpawnGroups` remains loaded as a compatibility baseline during migration.
