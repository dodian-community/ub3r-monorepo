# Game Server Plugins, Definitions, and Reference Data

Server-side gameplay assets are separated by ownership and runtime purpose.

## `game-server/plugins`

Independently compiled, descriptor-discovered gameplay modules live here.

- `platform/{api,runtime,testkit}` contains engine-independent contracts and shared fixtures.
- `skills/` contains the skill API/runtime/testkit and one module per skill.
- `quests/` contains the quest API/runtime/testkit and quest modules.

Skill and quest TOML belongs in its owning module's `src/main/resources`. Physical
location does not change the familiar Gradle paths such as `:skills:mining` and
`:quests:tutorial-island`.

## `game-server/definitions`

Tracked, hand-maintained files loaded by the game server:

- `combat/`: spell and projectile definitions.
- `doors/`: coordinate-specific door behavior.
- `items/`: item definitions, equipment appearance, and ground spawns.
- `objects/`: object removals, examines, and world-spawn TOML.

Loaders resolve this directory consistently whether the process starts from the
repository root or from `game-server`.

## `game-server/reference/cache-rev218`

Generated revision-218 cache reference material. Rich RSCM blocks, IntelliJ
indexes, interfaces, JSONL metadata, and the generation manifest live here.
These files are never loaded by the game server and never determine gameplay
IDs. Regenerate them with `:game-server:generateRscm`; verify them with
`:game-server:checkRscm`.

## `game-server/data`

Runtime cache and generated raw definition inputs. These files are not
hand-maintained gameplay content:

- `data/cache/`: binary cache files.
- `data/def/`: generated cache/wiki definition dumps and manifests.

`Settings.toml`, database assets, and environment configuration retain their
existing locations and are not gameplay definitions.
