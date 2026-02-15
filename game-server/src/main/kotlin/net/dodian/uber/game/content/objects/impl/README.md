# Object Content Layout

Object gameplay is implemented in Kotlin content modules.
`ObjectInteractionListener` is IO-only: packet decode, walk task scheduling, distance/pathing checks, then dispatch to content.

## Domains

- `agility/GnomeCourseObjects.kt`
- `agility/BarbarianCourseObjects.kt`
- `agility/WildernessCourseObjects.kt`
- `agility/WerewolfCourseObjects.kt`
- `banking/BankBoothObjects.kt`
- `banking/BankChestObjects.kt`
- `cooking/RangeObjects.kt`
- `crafting/SpinningWheelObjects.kt`
- `crafting/ResourceFillingObjects.kt`
- `doors/DoorToggleObjects.kt`
- `events/PartyRoomObjects.kt`
- `farming/FarmingPatchObjects.kt`
- `farming/CompostBinObjects.kt`
- `farming/FarmingPatchGuideObjects.kt`
- `mining/MiningRocksObjects.kt`
- `mining/GemRocksObjects.kt`
- `mining/SpecialMiningObjects.kt`
- `prayer/AltarObjects.kt`
- `runecrafting/RunecraftingObjects.kt`
- `smithing/FurnaceObjects.kt`
- `smithing/AnvilObjects.kt`
- `thieving/ChestObjects.kt`
- `thieving/StallObjects.kt`
- `thieving/PlunderObjects.kt`
- `travel/LadderObjects.kt`
- `travel/StaircaseObjects.kt`
- `travel/PassageObjects.kt`
- `travel/TeleportObjects.kt`
- `travel/WebObstacleObjects.kt`
- `woodcutting/WoodcuttingTreesObjects.kt`

## Object System Services

Core services are in:
- `/Users/alexismaneely/Desktop/ub3r-monorepo/game-server/src/main/kotlin/net/dodian/uber/game/content/objects/services/`

Provided capabilities:
- `objectId` and `objectId + position` binding resolution
- global object spawn/replace/revert lifecycle
- personal per-player object lifecycle
- clip flag apply/remove helpers
- unified click/use-item/magic dispatch context

## Authoring Rules

- Use mechanic-first files (not region-first) unless a type file becomes too large.
- Bind handlers with explicit IDs and position-aware bindings where needed.
- Keep gameplay in content modules; keep listener logic pathing/IO only.
