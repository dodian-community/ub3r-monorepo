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
- The architecture tests in `game-server/src/test/kotlin/net/dodian/uber/game/architecture` are contract docs: they guard against direct `TickTasks` coupling, broad `catch (Exception)` in core paths, and other boundary breaks.

## When changing core/runtime
- Use `Server.java` and `GameLoopService` as the source of truth for startup/shutdown flow. If you add a long-lived service, wire it into `Server.shutdown()`.
- `GameLoopService` is the 600ms scheduler; `GameTaskRuntime` and `TaskCoroutineFacade` sit on top of it. Do not add a second scheduler for gameplay tasks.
- `Database.kt` owns the HikariCP/MyBatis pool and `DbTables` table-name prefixing; `DatabaseInitializer.kt` imports `game-server/database/` when `DATABASE_INITIALIZE=true`.
- Avoid broad exception catches in engine/persistence/interaction code unless a test or existing pattern explicitly requires it.

## Build, test, and modules
- Useful commands: `./gradlew build`, `./gradlew :game-server:run`, `./gradlew :game-server:test`, `./gradlew :game-server:syncTest`, `./gradlew :game-server:runSyncBenchmark`.
- Java targets differ by module: `game-server` and `stress-client` use Java 17, `game-launcher-seamless` uses Java 11, and `game-client`, `game-launcher`, and `mystic-updatedclient` are Java 8.
- Other directories worth knowing: `game-server/database/` (SQL bootstrap scripts), `game-server/data/` (runtime content/config files), `game-server/scripts/` (generation utilities), `game-server/src/test/kotlin/` (architecture and runtime tests), `web-registration-simple/` (PHP site), and the small `web-api` module.

## Read first for context
- `docs/guides/getting_started.md`
- `docs/guides/installing_mysql.md`
- `docs/other/environment_variables.md`
- `docs/guides/task_coroutine_system.md`
- `docs/contribution/guidelines.md`


