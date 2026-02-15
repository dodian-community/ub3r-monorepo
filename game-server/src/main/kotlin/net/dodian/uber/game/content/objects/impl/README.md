# Object Content Migration Layout

This folder is the target structure for object gameplay content.
Core object infrastructure now lives in:
- `/Users/alexismaneely/Desktop/ub3r-monorepo/game-server/src/main/kotlin/net/dodian/uber/game/content/objects/services/`

## Domains

- `banking/BankBoothObjects.kt`
- `banking/BankChestObjects.kt`
- `travel/StaircaseObjects.kt`
- `travel/LadderObjects.kt`
- `travel/PassageObjects.kt`
- `travel/TeleportObjects.kt`
- `agility/GnomeCourseObjects.kt`
- `agility/BarbarianCourseObjects.kt`
- `agility/WildernessCourseObjects.kt`
- `agility/WerewolfCourseObjects.kt`
- `smithing/FurnaceObjects.kt`
- `smithing/AnvilObjects.kt`
- `prayer/AltarObjects.kt`
- `mining/MiningRocksObjects.kt`
- `mining/GemRocksObjects.kt`
- `mining/SpecialMiningObjects.kt`
- `runecrafting/RunecraftingObjects.kt`
- `thieving/ChestObjects.kt`
- `thieving/PlunderObjects.kt`
- `thieving/StallObjects.kt`
- `farming/FarmingPatchGuideObjects.kt`
- `doors/DoorToggleObjects.kt`
- `utility/UtilityObjects.kt`
- `legacy/LegacyResidualObjects.kt` (temporary low-priority residual fallback via content-dispatch)

## Current migration status

- Consolidated listener routes opcodes `132`, `252`, `70`, `192`, `35` through object content first.
- Consolidated listener now also routes `234` (click4) and `228` (click5) through the same object path.
- Position-aware object binding resolution is in place (`objectId + optional position/range/predicate`).
- Ported and bypassed from legacy:
  - Banking booth/chest/refund click flows in typed banking files
  - Furnace/anvil + item-on-object + orb charging magic in typed smithing files
  - Mining rocks cluster (`Utils.rocks`) in `MiningRocksObjects`
  - Prayer altar restore cluster
  - Gnome/wilderness/barbarian/werewolf agility first-click course handlers
  - Core stairs/ladders state-setting cluster
  - Major travel teleports and coordinate-specific passages
  - Pyramid Plunder object flow (doors/urns/chests/entry-exit + reset)
  - Thieving chest first-click loot flows + chest/stall second-click clusters
  - Obelisk second-click teleport (`823`)
- Unported object behavior is now routed through `LegacyResidualObjects` as a lower-priority content fallback.

## Completion checkpoint

- This branch is at a reviewable architecture/layout checkpoint.
- Legacy listeners are no longer loaded as packet registrations for object opcodes.
- Remaining unported behavior executes through content-dispatch fallback (`LegacyResidualObjects`) until every branch is fully type-file migrated.

## Requirement mapping

- `1) objectOnPosition`
  - Supported by position-aware bindings in `ObjectBinding` (`objectId` + exact tile, range, or predicate).
- `2) clip flag (noclip/clip)`
  - Supported by `ObjectClipService` (`apply` / `remove`) using region clipping APIs.
- `3) globalObjects lifecycle`
  - Supported by `ObjectSpawnService` for timed world object spawn/replace/revert.
- `4) personalObjects lifecycle`
  - Supported by `PersonalObjectService` for per-player display and timed revert.
- `5) pathing to interaction`
  - Kept in Netty listener flow (`ObjectInteractionListener`) with decode + walk + distance checks.
- `6) customObjectSpawns policy`
  - Permanent map edits should be cache-first.
  - Runtime services should be used for temporary or conditional overrides only.

## Authoring rules

- Each object content file should bind by:
  - `objectId`, or
  - `objectId + position` (exact/range/predicate).
- Each file may handle any interaction type needed:
  - click options 1-5
  - item-on-object
  - magic-on-object

## Review focus

- Domain split granularity (`travel` vs `agility` vs `runecrafting`)
- Whether stair state logic should stay in one file or split by region
- Any naming preferences before larger migration batches
