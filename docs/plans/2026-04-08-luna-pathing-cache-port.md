# Luna Pathing + Cache Decoder Port Implementation Plan

**Status:** Revised after implementation review on 2026-04-08.

**Goal:** Replace Dodian’s remaining legacy movement/clipping/cache behavior with a Luna-style pathing, collision, follow, and cache decode pipeline that boots cleanly and logs successful map/tile/object decode against the existing `game-server/data/cache` store.

**Corrected scope:** We are no longer treating this as a greenfield 9-task scaffold. The repository already contains first-pass scaffolding for direction encoding, pathfinding primitives, collision stubs, cache bootstrap stubs, follow state, sync tests, and architecture guards. The remaining work is to **wholesale port Luna’s real player-following behavior and Luna’s real cache decoding/loading behavior** into Dodian-owned packages, then wire them into the current server startup and tick/update pipeline. If cache decode mismatches appear, use `mystic-updatedclient` as the reference for how this cache is actually read and decoded.

**Architecture:**
- Keep all heavy cache decode/bootstrap work off the 600ms game tick.
- Decode cache data during startup, store immutable decoded tables, and build runtime collision from those tables before the server starts accepting play.
- Keep follow and interaction movement tick-driven in pre-update / entity processing.
- Preserve Dodian networking/update code where possible; only port Luna gameplay/runtime primitives.

**Primary references:**
- Luna server pathing/collision/cache:
  - `luna-master/src/main/java/io/luna/game/model/path/*`
  - `luna-master/src/main/java/io/luna/game/model/collision/*`
  - `luna-master/src/main/java/io/luna/game/model/mob/WalkingQueue.java`
  - `luna-master/src/main/java/io/luna/game/cache/*`
  - `luna-master/src/main/java/io/luna/game/cache/codec/MapDecoder.java`
- Mystic client cache/terrain/object decode references:
  - `mystic-updatedclient/src/com/runescape/cache/FileStore.java`
  - `mystic-updatedclient/src/com/runescape/cache/FileArchive.java`
  - `mystic-updatedclient/src/com/runescape/net/requester/ResourceProvider.java`
  - `mystic-updatedclient/src/com/runescape/scene/MapRegion.java`

---

## Progress inventory

### Already in repository
These items already exist and should be treated as scaffolding / characterization, not as remaining greenfield tasks:
- Direction mapping guard test: `DirectionAndFollowProtocolTest`
- Initial pathfinding primitives:
  - `Heuristic.kt`
  - `Node.kt`
  - `PathfindingAlgorithm.kt`
  - `AStarPathfindingAlgorithm.kt`
  - `SimplePathfindingAlgorithm.kt`
- Initial collision scaffolding:
  - `CollisionFlag.kt`
  - `CollisionMatrix.kt`
  - `CollisionUpdate.kt`
  - `CollisionManager.kt`
- Initial cache scaffolding:
  - `CacheStore.kt`
  - `MapIndexTable.kt`
  - `MapDecoder.kt`
  - `CacheBootstrapService.kt`
  - `CollisionBuildService.kt`
- Initial follow scaffolding:
  - `FollowState.kt`
  - `FollowService.kt`
  - `PlayerClickListener.java`
- Sync / architecture tests already added:
  - `InteractionMaskEncodingTest`
  - `CacheBootstrapLoggingTest`
  - `NoRangableArchitectureTest`
  - `LegacyCachePackageBoundaryTest`
- Legacy shims already removed:
  - `Rangable.java`
  - old cache shim files under `net/dodian/cache/...`

### Corrections from implementation review
The scaffolding above is **not feature complete**. Critical gaps:
1. `CacheStore.kt` is still a flat-file reader; the real cache is the standard sector-based `main_file_cache.dat` + `main_file_cache.idx*` format.
2. `MapDecoder.kt` only reads a stub `map_index`; it does not decode real archive-backed map tiles or objects.
3. The current collision system is only a wall/solid stub, not Luna’s full directional collision model.
4. `FollowService.kt` still drives movement with a simple direct walk intent instead of Luna-style path-aware follow routing around collision.
5. Cache bootstrap logging currently reports placeholder totals instead of real decoded region/tile/object totals.

---

## Execution rules

- Use test-driven development for every behavior change.
- Run targeted tests after each slice, then broader verification once the slice is stable.
- No blocking I/O in Netty I/O threads or the 600ms game loop.
- Keep the port in Dodian-owned packages.
- Prefer Kotlin for new server code.
- Keep commit scope small: one real implementation slice per commit.

---

## Revised implementation sequence

### Slice 1: Replace the flat cache reader with a real sector-based cache store
**Status:** Completed on 2026-04-08.

**Why first:** Everything else depends on reading the real cache correctly.

**Files:**
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/systems/cache/CacheStore.kt`
- Create: `game-server/src/main/kotlin/net/dodian/uber/game/systems/cache/Archive.kt`
- Create: `game-server/src/main/kotlin/net/dodian/uber/game/systems/cache/CacheUtils.kt`
- Modify: `game-server/src/test/kotlin/net/dodian/uber/game/systems/cache/MapDecoderSmokeTest.kt`
- Create or modify focused cache-store tests under `game-server/src/test/kotlin/net/dodian/uber/game/systems/cache/`

**Requirements:**
- Port the sector-chain read protocol from Luna / Mystic (`main_file_cache.dat` + `.idx*`).
- Support archive `0` file `5` (`versionlist`) so `map_index` can be extracted from the archive.
- Port archive decoding and hashed entry lookup (`map_index`, `midi_index`, etc.).
- Keep API surface small and Dodian-owned.

**Done when:**
- A targeted test proves `CacheStore` can read a synthetic sector-chain file.
- `MapDecoderSmokeTest` reads `map_index` through the real archive-backed path.
- Boot logging still passes.

### Slice 2: Port Luna map index, map tile, and map object decoding
**Status:** Completed on 2026-04-08 for archive-backed region index/tile/object decode and bootstrap summary logging.

**Files:**
- Modify: `MapDecoder.kt`
- Modify: `MapIndexTable.kt`
- Add decoded model types for tiles/objects/summary data
- Expand cache tests

**Requirements:**
- Decode `map_index` from archive contents.
- Decode landscape/tile archives from store index 4.
- Decode object archives from store index 4.
- Match Luna behavior first; if mismatches appear, reconcile using Mystic `ResourceProvider` and `MapRegion`.

**Done when:**
- Real cache decode produces non-zero region/tile/object totals.
- Summary logs report actual totals, not placeholders.

### Slice 3: Replace collision stubs with Luna directional collision
**Status:** Completed on 2026-04-08 for directional collision primitives and decoded terrain/object collision build.

**Files:**
- Rewrite `systems/pathing/collision/*`
- Integrate decoded tiles/objects into runtime collision build
- Update collision tests

**Requirements:**
- Port directional mob/projectile flags.
- Port traversal and reached checks needed for movement/follow/interactions.
- Mark blocked/bridged tiles from decoded terrain.
- Apply wall/object collision based on object type, rotation, size, solidity, and walkability.

### Slice 4: Wholesale port Luna follow movement into Dodian tick processing
**Status:** Completed on 2026-04-08 for collision-backed tick-driven follow routing into canonical walk-command buffers.

**Files:**
- Modify `FollowService.kt`
- Modify `EntityProcessor.kt`
- Modify listeners only as needed
- Add / expand follow routing tests

**Requirements:**
- Keep follow intent packet-driven but movement tick-processed.
- Route follow movement through pathfinding/collision rather than direct straight-line enqueueing.
- Preserve face-target update behavior and stop/reset semantics.
- Prefer Luna’s follow/walking semantics, adapted to Dodian’s queue/update model.

### Slice 5: Final bootstrap/runtime cutover and verification
**Status:** Completed on 2026-04-08 for decoded-cache startup collision rebuild, collision-ready logging, and startup guardrails.

**Files:**
- Modify `CacheBootstrapService.kt`
- Modify `Server.java`
- Finalize architecture/boundary tests

**Requirements:**
- Startup decodes cache, builds collision, logs real totals, then starts runtime.
- No remaining `Rangable` or removed-cache-shim startup dependencies.
- Follow movement works around walls in runtime smoke testing.

---

## Immediate next task

Implement **Slice 6** now:
1. Complete verification for the new startup overlay replay of DB objects + `DoorRegistry` collision onto `CollisionManager.global()`.
2. Audit remaining world-mutation paths (temporary globals, personal objects, special-case replacements) and decide which must also update shared collision.
3. Add or run a startup smoke verification path for decoded-cache, collision-ready, and startup-overlay log lines.
4. Run targeted interaction/pathing tests against the updated runtime collision state.
5. If safe in the local environment, run `:game-server:run` and confirm the startup collision log lines.

---

## Verification checklist

### After Slice 1
- `./gradlew :game-server:test --tests "*CacheStore*" --tests "*MapDecoderSmokeTest" --tests "*CacheBootstrapLoggingTest"`

### After Slice 2+
- `./gradlew :game-server:test --tests "net.dodian.uber.game.systems.cache.*"`

### Before completion
- `./gradlew :game-server:test`
- `./gradlew :game-server:run`

Expected startup logs should eventually include:
- `Cache decode complete: regions=... tiles=... objects=...`
- `World collision ready from decoded cache.`

---

## Implementation notes

- The existing `game-server/data/cache` directory is a standard sector-based cache store (`main_file_cache.dat` + `main_file_cache.idx*`), not a flat directory of extracted files.
- `map_index` is loaded from the version-list archive, not from a root-level standalone file in this repo.
- If Luna’s archive/map decode disagrees with the actual cache, prefer the mystic client’s decode behavior for the concrete binary format and Luna’s server structures for runtime behavior.
- Do not port Luna networking abstractions; adapt only the relevant gameplay/runtime/cache pieces.
