# Skill Plugin Modules

Gameplay skills live in independent Gradle modules under `game-server/plugins/skills/`. A module must
apply `ub3r.skill-plugin`, declare its implementation class, own its route
bindings, bundle TOML data, and test its behavior without a `Client` instance.

```kotlin
plugins { id("ub3r.skill-plugin") }

skillModule {
    implementationClass.set("net.dodian.uber.skills.example.ExampleModule")
}
```

Use `SkillPlugin` for a trainable skill and `SkillContentModule` for supporting
content such as Skill Guide. The convention plugin generates the module
descriptor consumed by startup discovery.

## Data

Keep data in `src/main/resources/<skill>/`. Every TOML file begins with
`schema_version = 1`, has one array-of-tables record kind, uses snake_case
fields, and has stable record keys. IDs, rates, requirements, and rewards are
data; policy choices and orchestration stay in Kotlin.

The shared reader fails on missing or malformed files. Module loaders must also
validate semantic constraints such as duplicate IDs, invalid levels, and broken
cross references.

## Tests

Use `FakeSkillPlayer` for route and action tests. Cover successful outcomes,
resource/level failures, cancellation, and transactional rollback. Run:

```text
./gradlew :skills:<name>:check
./gradlew skillsCheck
```

Modules may not import `Client`, unwrap engine adapters, or depend on
`:game-server`. Engine packet rendering, persistence wiring, and world access
belong in thin adapters only.

## Real-world integration tests

`FakeSkillPlayer` tests are mandatory per module (`VerifySkillModule` fails the
build without one) and stay fast because they never touch a real `Client`,
`World`, or tick loop. For extra end-to-end confidence on a skill's most
important flows, there is a second, additive tier that boots a real in-process
game world - real cache-loaded map/collision/object defs, a real
`GameLoopService` tick pipeline, real skill-plugin wiring - and drives actions
through the same packet-equivalent entry points a real client uses
(`PacketObjectService.handleObjectClick`/`handleItemOnObject`, etc.). This
tier lives in `:game-server`, not in the plugin modules, since it needs
`Client`/`World`, which skill modules may not depend on.

See `game-server/src/test/kotlin/net/dodian/uber/game/integration/skills/`:
`support/RealWorldHarness.kt` (the boot/tick/factory harness, including
`objectOption`/`itemOnObject`/`npcOption`/`itemOnItem`/`itemClick`/
`completeProduction` entry points) and `support/RealWorldSkillTest.kt` (the
base test class). 14 of the 16 skill modules have a real-world test file
covering their primary interaction shape:

| Interaction shape | Skills |
| --- | --- |
| object click (gathering loop) | Woodcutting, Mining |
| object click (one-shot) | Runecrafting, Agility |
| npc click | Fishing, Thieving |
| item-on-object | Smithing, Cooking, Prayer (offering) |
| item-on-item (direct) | Firemaking, Crafting, Slayer (helm) |
| item-on-item (make-x menu, `completeProduction`) | Fletching, Herblore |
| item click | Prayer (bury) |

Note on Agility: `SkillWorld.traverse`/`climb` complete via
`GameEventScheduler.runLaterMs(durationMs)`, which looks like a wall-clock
scheduler at a glance but actually converts `durationMs` to ticks
(`delayMsToTicks`) and waits on `GameTaskRuntime`'s world-task queue - the
same queue `GameLoopService.runTick()`'s `world.tasks` phase cycles every
tick (`ActionProcessor.run()` -> `QueueTaskService.processDue()` ->
`GameTaskRuntime.cycleWorld()`). So it's driven by this tier's ordinary
`tick()`/`tickUntil()` like everything else - no real sleep needed. Also note
`AgilityModule.crossObstacle` requires the player to be standing at the
obstacle's exact real-world `startPosition`, so its success test spawns the
player at that real coordinate rather than an arbitrary one.

**Not covered, deliberately deferred:**
- **Farming** - blocked on a real, suspected-live bug, not just missing test
  infrastructure. `interactItemBin`/`clickPatch`
  (`engine/systems/skills/farming/Farming.kt`) classify a clicked patch's
  crop type by parsing `GameObjectData.forId(objectId).name` (stripping
  `" patch"`, e.g. `"allotment patch"` -> `"allotment"`). Verified empirically
  against the real bundled cache: all 17 ids in
  `FarmingModule.livePatchObjectIds` - the ones actually wired into
  `FarmingData.patches`, the internal table the interaction loop matches
  against - resolve to the literal string `"null"` for `.name` (not an actual
  null; `GameObjectData.name` is non-nullable), so crop-type classification
  fails silently for every currently-configured real patch location: raking
  works (doesn't need the name), planting silently no-ops (needs it). A
  separate set of ids (8573/7840/8132, `FarmingModule.patchObjectIds`) do
  resolve to real names ("Allotment"/"Flower Patch"/"Herb patch") but aren't
  in `FarmingData.patches` either, so they don't dispatch at all. This needs
  its own fix (confirm real per-location object ids against a live client,
  and/or fix name resolution for these multiloc-child objects) before a
  meaningful real-world test can be written - see the spawned task for
  tracking. Also note growth/harvest (separate from planting) runs on real
  elapsed wall-clock time via `FarmingRuntimeService`'s offline "catch-up
  pulse" system, not ticks - relevant for whoever picks this up next.
- **Skill Guide** - a `SkillContentModule` (UI content, not a trainable
  skill), better suited to unit-level interface tests than this tier.

This tier is not mandatory per module - add one for a skill when its flow is
representative of a new interaction shape or high-risk enough to warrant
real-pipeline coverage, not for every skill by default. It requires the real
OSRS cache under `game-server/data/cache` and skips gracefully without it
(e.g. in CI) via `RequiresCache`. Run locally with:

```text
./gradlew :game-server:realWorldSkillTest
```
