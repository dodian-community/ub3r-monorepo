# Skills object/NPC compatibility audit

## Baseline

This report is generated from the active `SkillPluginDefinition` route registrations, not a
separate list of gameplay IDs. It resolves each route through the committed rev218
`loc.rscm`/`npc.rscm` mappings and can compare it with the Tarnish client-era reference:

```sh
./gradlew :game-server:runSkillTargetAudit \
  -PtarnishRoot='/Users/tylercovalt/Desktop/RSPS/RSPS server resoures/tarnish-main/game-server'
```

The command prints the complete current inventory, including every object click,
item-on-object, magic-on-object, and NPC click binding from active skill plugins. It also
lists standard skill interface/button routes and includes the engine-owned furnace interface
`2400`. Item IDs and the custom `makeall` presentation are deliberately absent.

## Comparison rules

| Result | Meaning | Required follow-up |
|---|---|---|
| `existing content remap` | A documented target-ID replacement preserves a current route and all gameplay behavior. | Retain the remap and its fixture. |
| `existing content verified` | Tarnish corroborates an already-registered local route. | Retain the binding. |
| `Tarnish-only` | Tarnish has a candidate not represented by current server content. | Report only; never register it. |
| `ambiguous` | Local/Tarnish evidence is insufficient to prove an existing-content remap. | Keep the binding and investigate manually. |

Tarnish's `global_objects.json` is a spawn catalogue, not a complete object-definition dump.
Therefore an object absent from that file is **not** evidence that the local ID is invalid.

## Initial evidence snapshot (2026-07-29)

| Skill route | Local rev218 mapping | Tarnish evidence | Status |
|---|---|---|---|
| Smithing furnace `16469` | Furnace, option 2 `Smelt` | Tarnish global-object spawn names it `furnace`. | Verified identity; retain. |
| Smithing anvil `2097` | Anvil, option 1 `Smith` | Tarnish contains both `furnace` and `anvil home` spawn labels for ID `2097`. | Multiple location-specific variants; no global replacement. |
| Cooking range `114` | Cooking range, option 1 `Cook` | Tarnish global-object spawn names it `Cooking range`. | Verified identity; retain. |
| Prayer altar `409` | Altar, option 1 `Pray-at` | Tarnish global-object spawn names it `Altar`. | Verified identity; retain. |
| Prayer altar `411` | Chaos altar, option 1 `Pray-at` | Tarnish BoneSacrifice accepts it, but this server has no equivalent active altar route. | Tarnish-only; not registered. |
| Crafting wheel `4309` | Spinning wheel, option 2 `Spin` | Tarnish Crafting opens spinning there, but this server does not currently expose that wheel. | Tarnish-only; not registered. |
| Fishing `1518` | Fishing spot (`Small Net`/`Bait`) | Tarnish `FishingSpot.SMALL_NET_OR_BAIT` uses `1518`; local options match. | Updated from legacy `1514`. |
| Fishing `1526` | Rod Fishing spot (`Lure`/`Bait`) | Tarnish `FishingSpot.LURE_OR_BAIT` uses `1526`; local options match. | Updated from legacy `1506`. |
| Fishing `1519` | Fishing spot (`Cage`/`Harpoon`) | Tarnish `FishingSpot.CAGE_OR_HARPOON` uses `1519`; local options match. | Updated from legacy `1510`. |
| Fishing `1511` | Fishing spot (`Net`/`Harpoon`) | No exact Tarnish `FishingSpot` role; server-specific monkfish/shark behavior remains. | Intentionally unchanged. |
| Fishing `1520` | Fishing spot (`Big Net`/`Harpoon`) | Tarnish `FishingSpot.LARGE_NET_OR_HARPOON` also uses `1520`. | Route verified; custom reward data remains unchanged. |

The stateful skills—Agility, Farming, Thieving, and Runecrafting—remain intentionally
unmodified until their per-tile object type/orientation and click-option evidence has been
reviewed. The audit reports their targets, but does not infer a replacement from a name alone.

## Interface review

The audit inventories server-owned skill controls only: prayer tab `5608`, skill guide `8714`
and its legacy button IDs, and the furnace chatbox `2400`. Their presence in Tarnish source is
an aid for packet review, not proof that a numeric hit elsewhere in source is an interface
handler. The custom make-all UI is excluded by design.
