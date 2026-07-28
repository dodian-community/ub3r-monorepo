# Revision-218 Cache Reference

This directory is a generated, human-readable reference for OSRS revision 218
(`0a55afe6182e9c6a22955650dd02fc7d08d1648c`). It is not loaded by the server
and never determines gameplay IDs.

## Layout

- Root `.rscm` files contain z-kris-style blocks for every text configuration
  dump in the pinned revision. Raw cache symbols are stable section names,
  readable same-revision names are `// alias=...` comments, and decoded fields
  remain in their original order. Repeated fields are deliberately retained.
- `interfaces/` mirrors all pinned `.if` and `.if3` files, one file per
  interface with every component field intact.
- `index/` contains compact, deterministic `key=id` mappings for IntelliJ and
  possible future loaders. Each namespace contains exactly one key per ID.
- `metadata/` contains JSONL records with ordered fields, grouped properties,
  right-click options, naming provenance, source files, and provable
  same-revision symbol references. `manifest.json` records source and artifact
  hashes.

Item certificate and placeholder aliases may inherit a decoded name only
through links in this pinned dump. Missing evidence remains numeric. Names from
other revisions, including `coalrock1` and `area_task`, are never imported.
Models, scripts, and binary assets are not copied, although their symbolic
references remain visible in the configuration fields.

## Terminology

- `obj` means inventory item.
- `loc` means world object/location.
- `seq` means animation sequence.
- `spotanim` means spot animation/graphic.
- `varp` and `varbit` are player-variable definitions.

## Regeneration

Generate the reference from the pinned dump:

`./gradlew :game-server:generateRscm -PosrsDump=/absolute/path/to/osrs-dumps-0a55afe6182e9c6a22955650dd02fc7d08d1648c`

Verify the committed output without rewriting it:

`./gradlew :game-server:checkRscm -PosrsDump=/absolute/path/to/osrs-dumps-0a55afe6182e9c6a22955650dd02fc7d08d1648c`

Generation rejects a different revision, missing or unexpected config dumps,
malformed records, duplicate IDs, mismatched components, and cyclic item naming
links.
