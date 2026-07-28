# RSCM Mappings Reference

This directory contains generated revision-218 cache-reference mappings for IntelliJ tooling, search, and reverse lookup. They are not loaded by the server and never determine gameplay IDs.

## Mappings Dictionary
Due to client cache naming conventions (derived from original Jagex client terminologies), some file extensions and prefixes use technical terms:

* **`obj.rscm` (Items)**: Maps Item IDs to their names. In original client cache naming, standard inventory items are referred to as "objs" or "object types" (not physical game objects).
* **`loc.rscm` (Game Objects / Locations)**: Maps physical game object IDs (such as doors, trees, chest, ladders, walls) to their names. In original client cache naming, these are referred to as "locations" or "locs".
* **`npc.rscm` (NPCs)**: Maps Non-Player Character IDs to their names.
* **`spotanim.rscm` (Spot Animations / GFX)**: Maps graphic effect IDs (such as spell impact animations, projectiles, etc.) to their names.
* **`seq.rscm` (Sequences / Animations)**: Maps animation/sequence IDs (such as fishing animations, weapon attack animations, etc.) to their names.
* **`varp.rscm` (Player Variables)**: Maps client/player setting config IDs to their names.
* **`varbit.rscm` (Variable Bits)**: Maps bits within variable settings to their names.

---

Skill gameplay plugins deliberately use reviewed raw cache IDs in their own
definitions. RSCM remains developer tooling for lookup and reverse lookup; it
is not consulted for skill route registration or runtime dispatch.

## Regeneration

Use the pinned dump as the source of truth:

`./gradlew :game-server:generateRscm -PosrsDump=/absolute/path/to/osrs-dumps`

The task emits compact `.rscm` name-to-ID files and rich JSONL records under
`metadata/`. Names come only from the pinned revision-218 dump: decoded names,
same-revision symbols, and verified item note/placeholder links. Records without
such evidence keep their numeric fallback names. JSONL includes every decoded
definition field plus a structured `options` object for right-click actions;
component records additionally include their complete interface definition and
source file. Verify committed output without writing it with
`./gradlew :game-server:checkRscm -PosrsDump=/absolute/path/to/osrs-dumps`.
