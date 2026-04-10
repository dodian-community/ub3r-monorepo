# Cache object-definition opcode inventory (`loc.dat`)

Source cache: `game-server/data/cache`

Scan source:
- script: `game-server/scripts/scan_loc_opcodes.py`
- generated inventory: `build/loc-opcode-inventory.json`

Scanned on: 2026-04-09

## Summary

- Object definitions scanned: `32558`
- Unique opcodes observed: `43`
- Unknown opcodes observed: `0`

## Opcode inventory

| Opcode | Objects | Entries | Notes |
|---|---:|---:|---|
| 0 | 32558 | 32558 | terminator |
| 1 | 11682 | 11682 | model ids + model types |
| 2 | 17315 | 17315 | name |
| 5 | 19098 | 19098 | model ids only |
| 14 | 5355 | 5355 | sizeX |
| 15 | 4943 | 4943 | sizeY |
| 17 | 3132 | 3132 | non-solid |
| 18 | 7182 | 7182 | non-impenetrable / projectile-passable hint |
| 19 | 17632 | 17632 | explicit interactive flag |
| 21 | 9115 | 9115 | contoured ground |
| 22 | 8884 | 8884 | delayed shading |
| 23 | 429 | 429 | occludes |
| 24 | 2455 | 2455 | animation id |
| 27 | 5716 | 5716 | cache-variant zero-payload flag |
| 28 | 173 | 173 | decor displacement |
| 29 | 12991 | 12991 | ambient lighting |
| 30 | 8453 | 8453 | action slot 0 |
| 31 | 2533 | 2533 | action slot 1 |
| 32 | 549 | 549 | action slot 2 |
| 33 | 1404 | 1404 | action slot 3 |
| 34 | 1570 | 1570 | action slot 4 |
| 39 | 5852 | 5852 | light diffusion |
| 40 | 11248 | 11248 | recolors |
| 41 | 622 | 622 | retextures |
| 62 | 1890 | 1890 | inverted |
| 64 | 1108 | 1108 | casts shadow false |
| 65 | 805 | 805 | scaleX |
| 66 | 2278 | 2278 | scaleY |
| 67 | 793 | 793 | scaleZ |
| 68 | 2266 | 2266 | mapscene |
| 69 | 1615 | 1615 | surroundings |
| 70 | 108 | 108 | translateX |
| 71 | 315 | 315 | translateY |
| 72 | 96 | 96 | translateZ |
| 73 | 2603 | 2603 | obstructs ground |
| 74 | 52 | 52 | hollow |
| 75 | 812 | 812 | supportItems / legacy walk-type field |
| 77 | 1728 | 1728 | varbit/varp transforms |
| 78 | 776 | 776 | ambient sound |
| 79 | 55 | 55 | ambient sound set |
| 81 | 9115 | 9115 | extra terrain/contour byte in this cache family |
| 82 | 98 | 98 | minimap function |
| 92 | 50 | 50 | transform variant with fallback child |

## Semantics that currently matter for server clipping/reach

The server currently needs only a subset of object-definition metadata for authoritative movement and interaction reach:

- `14`, `15` → footprint (`sizeX`, `sizeY`)
- `17` → non-solid
- `19` plus model-data fallback (`1` / `5`) → `hasActions`
- `30..34` → action presence also contributes to `hasActions`
- `74` → hollow objects clear solidity

Additional notes:

- `18` is important for future ranged / projectile LOS parity, because it means the object is not impenetrable even if it may still be solid for movement.
- `73` is an obstructive-ground hint; it is recorded today but does not currently change collision flags directly.
- `27`, `89`, `60`, `61`, and `249` are treated as compatibility / metadata opcodes in the current server decoder so cache bootstrap does not fail on this cache variant.

## Current decoder alignment status

`game-server/src/main/kotlin/net/dodian/uber/game/systems/cache/ObjectDefinitionDecoder.kt` now:

- accepts every opcode observed in this cache
- handles the cache-variant opcode `27`
- derives `hasActions` from model metadata when opcode `19` is absent, matching client behavior more closely
- preserves action-slot scanning in the client-style `30..38` range, even though this cache currently only uses `30..34`

## Follow-up opportunities

If ranged / magic LOS parity is implemented later, the next object-definition field to thread through server collision is opcode `18` (impenetrable vs merely solid).

