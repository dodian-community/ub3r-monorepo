# AGENTS.md

## Big picture
- This is a multi-module Gradle monorepo. `game-server` is the runtime; the other modules are clients/launchers, `stress-client`, `ksp-processor`, and supporting tooling.
- New server code should default to Kotlin in `game-server/src/main/kotlin`; `game-server/src/main/java` is mostly legacy/bootstrap/interop and should shrink over time.
- Start with `README.md`, `settings.gradle.kts`, `game-server/build.gradle.kts`, and `game-server/src/main/java/net/dodian/uber/game/Server.java`.
- Boot order is: env config (`DotEnv.kt`) -> optional SQL import (`DatabaseInitializer.kt`) -> cache/content bootstrap -> Netty game server -> 600ms game loop -> Spark status API (`WebApi.kt`).
- Treat this file as a living guide: when you discover durable repo knowledge that will help future agents (conventions, workflows, boundaries, or recurring gotchas), add a concise note to the most relevant section here.

## Kotlin directory map
- `game-server/src/main/kotlin/net/dodian/uber/game/engine`: runtime infrastructure (`loop`, `tasking`, `phases`, `scheduler`, `sync`, `net`, `lifecycle`, `metrics`). Put timing, threading, and server plumbing here.
- `.../model`: domain objects and value types (`entity`, `player`, `item`, `object`, `chunk`, `Position.kt`, `EntityType.kt`). Keep this layer free of DB/network dependencies.
- `.../content`: gameplay features (`commands`, `dialogue`, `events`, `items`, `minigames`, `npcs`, `objects`, `shop`, `skills`, `ui`). Put behavior here, not in engine code.
- `.../systems`: orchestration and adapters. `api/content` is the stable content-facing surface; `dispatch` handles bootstrap/indexing; `combat`, `interaction`, `world`, `plugin`, and `net` are cross-cutting coordinators.
- `.../persistence`: database and save/load adapters (`db`, `repository`, `world`, `account`, `audit`, `admin`, `player`). Keep SQL/MyBatis/Hikari concerns here.
- `.../events`: event contracts and listeners by domain (`combat`, `item`, `magic`, `npc`, `player`, `skilling`, `trade`, `widget`, etc.).
- `.../tasks/TickTasks.kt`: legacy tick task surface. Prefer the coroutine facade for new work.
- `game-server/src/main/java`: legacy Java entrypoints and compatibility code (`Server.java`, `Constants.java`, `netty/`). Prefer Kotlin when adding new server code.

## DDD-style layering rules
- Dependencies should flow inward: `content` uses `model` + `systems.api`; `systems` orchestrates domain behavior; `engine` provides runtime mechanics; `persistence` is an adapter layer.
- `ContentRuntimeApi`, `ContentScheduling`, and `ContentTiming` are the preferred Kotlin entrypoints for gameplay code. `ContentBootstrap` + `ContentModuleIndex` show how features are discovered and bootstrapped.
- Generated plugin/module wiring comes from KSP (`ksp-processor` + `GeneratedPluginModuleIndex`), so manual registration should be a last resort.

## When adding content
- Put gameplay logic in `content/*` and wire it through `systems/api/content` rather than calling engine internals directly.
- For delays or repeating flows, use `worldTaskCoroutine` / `playerTaskCoroutine` / `npcTaskCoroutine`, or the content wrappers in `ContentScheduling`.
- `delay(...)` is in game ticks, not milliseconds. The server tick is `600ms`.
- Keep blocking DB/file/network work out of task blocks. If content needs persistence, call into persistence services or async helpers instead of doing I/O inline.
- Player follow is now intent-driven by packet listeners but route computation happens in `systems/follow/FollowService` on the game tick using `AStarPathfindingAlgorithm` + the global directional `CollisionManager`; follow writes canonical `newWalkCmd*` buffers rather than calling direct straight-line run helpers.
- Player right-click slot/opcode mapping is currently nonstandard for parity with this client: slot `4` is `Follow` (opcode `139`) and slot `5` is `Trade with` (opcode `39`). Keep menu labels and listener routing aligned together when changing either side.
- The architecture tests in `game-server/src/test/kotlin/net/dodian/uber/game/architecture` are contract docs: they guard against direct `TickTasks` coupling, broad `catch (Exception)` in core paths, and other boundary breaks.

## When changing core/runtime
- Use `Server.java` and `GameLoopService` as the source of truth for startup/shutdown flow. If you add a long-lived service, wire it into `Server.shutdown()`.
- `GameLoopService` is the 600ms scheduler; `GameTaskRuntime` and `TaskCoroutineFacade` sit on top of it. Do not add a second scheduler for gameplay tasks.
- `Database.kt` owns the HikariCP/MyBatis pool and `DbTables` table-name prefixing; `DatabaseInitializer.kt` imports `game-server/database/` when `DATABASE_INITIALIZE=true`.
- `game-server/data/cache` is a sector-based cache store (`main_file_cache.dat` + `main_file_cache.idx*`); `map_index` is loaded from the version-list archive (store `0`, file `5`), not from a flat root file.
- `GameObjectData` is now hydrated during `CacheBootstrapService` from the config archive in store `0`, file `2` (`loc.dat` / `loc.idx`). If clipping or object reach looks wrong, inspect the cache definition decode first before adding more fallback metadata.
- To audit the live cache’s object-definition opcode set, run `game-server/scripts/scan_loc_opcodes.py`; the current inventory is documented in `docs/other/cache_object_definition_opcode_inventory.md`.
- `map_index` in this repo is count-prefixed and currently decodes as 7-byte entries (`region`, `landscape`, `object`, `priority`). Landscape/object region archives are read from store `4` and are GZIP-compressed.
- `systems/pathing/collision` now uses directional mob/projectile flags instead of the old wall/solid stub; decoded terrain bridges lower blocked tiles by one plane, mirroring the client cache scene build behavior.
- `CollisionManager.isTileBlocked` and `isAnyFullBlocked` use `hasAllFlags` (not `hasFlags`) against `FULL_MOB_BLOCK`; a tile with only directional wall flags must NOT be treated as a full solid block.
- After `CacheBootstrapService` rebuilds static cache collision, `Server.java` now replays DB-backed `Server.objects` (currently plane `0` only from `gameobject_definitions`) plus `DoorRegistry` overlays into `CollisionManager.global()` via `ObjectClipService.bootstrapStartupOverlays(...)`; runtime door toggles refresh collision through the same service.
- Global cache-object removals/replacements that are meant to change gameplay collision must go through `systems/interaction/StaticObjectOverrides` + `ObjectClipService` startup replay, not just client-side `ReplaceObject2(...)` calls in login/setup code; otherwise players will see deleted doors but still collide with the old cache wall.
- Player-only opened blockers (e.g. special gates/web-like passages) should pair a visual override such as `PersonalObjectService` with a short-lived movement-layer grant such as `systems/interaction/PersonalPassageService`; do not mutate global collision or teleport the player just to simulate passage.
- Avoid broad exception catches in engine/persistence/interaction code unless a test or existing pattern explicitly requires it.
- `PlayerMovementState.getNextWalkingDirection` now validates each queued step against `CollisionManager.global().traversable` before moving the player; if the step is blocked, the walking queue is cleared and the player stops. This prevents wall clipping for all server-side movement (follow, combat follow, and any code that writes to `newWalkCmdX/Y`).
- `FollowService` now mirrors Luna-style `walkBehind`: on repath it targets the tile behind the target's last accepted walk delta (`lastWalkDeltaX/Y`), validates each A* step with `CollisionManager.traversable`, and enqueues the validated route into canonical `newWalkCmd*` buffers (up to walking-queue capacity). If follower and target overlap, follow first nudges the follower one random traversable cardinal tile.
- Player-vs-player follow-facing now uses `setFocus(...)` / `FACE_COORDINATE` instead of `facePlayer(...)` so observers keep walking-direction animations; newly added locals recover their idle facing from `Entity.persistedFaceX/Y`, which `PlayerMovementState` refreshes after each accepted step.
- Follow pathfinding now uses the Euclidean heuristic and cancels when Euclidean distance reaches 15+ tiles (Luna parity). Manual click-walk packets (`164`/`248`) cancel follow intent immediately so players can unfollow by walking.
- `PacketWalkingService` populates `newWalkCmd*` before the manual-unfollow branch. If a walk packet (`164`/`248`) needs to break follow, only remove follow intent/face state; clearing queued walking there will wipe both tile-click and minimap walking.

## Build, test, and modules
- Useful commands: `./gradlew build`, `./gradlew :game-server:run`, `./gradlew :game-server:test`, `./gradlew :game-server:syncTest`, `./gradlew :game-server:runSyncBenchmark`.
- **Working directory**: when running via `./gradlew :game-server:run` or the fat JAR, the working directory is `game-server/`. Runtime file paths must be relative to that (e.g. `data/cache`, not `game-server/data/cache`).
- Java targets differ by module: `game-server` and `stress-client` use Java 17, `game-launcher-seamless` uses Java 11, and `game-client`, `game-launcher`, and `mystic-updatedclient` are Java 8.
- Other directories worth knowing: `game-server/database/` (SQL bootstrap scripts), `game-server/data/` (runtime content/config files), `game-server/scripts/` (generation utilities), `game-server/src/test/kotlin/` (architecture and runtime tests), `web-registration-simple/` (PHP site), and the small `web-api` module.

## Read first for context
- `docs/guides/getting_started.md`
- `docs/guides/installing_mysql.md`
- `docs/other/environment_variables.md`
- `docs/guides/task_coroutine_system.md`
- `docs/contribution/guidelines.md`


