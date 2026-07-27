# Game Server Data & Content Layout

To maintain a clean division of concerns and a developer-friendly workflow, the server assets are divided into two top-level directories: `/game-server/content/` and `/game-server/data/`.

---

## 1. Content Directory (`/game-server/content/`)
**Git Status**: Tracked (Committed to Git)  
**Access**: Actively maintained and edited by content developers.

This directory contains configurations that control gameplay features, items, objects, combat parameters, and map details.

### `/content/items/`
- **`item_definitions.json`**  
  *Format*: JSON Array  
  *Purpose*: Contains all hand-maintained gameplay item metadata, bonuses, stats, shop values, noted item structures, weight, and requirements.  
  *Editing*: Edit this file whenever you need to adjust item stats, values, or basic properties.
- **`equipment_appearance.toml`**  
  *Format*: TOML Array of `[[equipment]]`  
  *Purpose*: Maps equipment item IDs to their rendering body appearance types (e.g., `HAT`, `BODY`, `HELM`, `FACE`, `MASK`). These flags determine which parts of the player's body models (like hair, beard, or arms) remain visible or are hidden when the item is equipped.  
  *Editing*: Add or modify entries here when new apparel or armour is added to ensure it renders correctly on the player character.

### `/content/objects/`
- **`object_examines.json`**  
  *Format*: JSON Array  
  *Purpose*: Stores custom examine string messages displayed when players click "Examine" on an in-game object.  
  *Editing*: Edit this file to add examine descriptions for custom or standard objects.
- **`removed.toml`**  
  *Format*: TOML Array of `[[remove]]`  
  *Purpose*: Lists coordinate ranges or specific single coordinates where static map objects loaded from the cache should be deleted/removed upon server boot.  
  *Editing*: Edit this file when cleaning up map layouts or clearing paths.

### `/content/doors/`
- **`doors.toml`**  
  *Format*: TOML Array of `[[door]]`  
  *Purpose*: Defines coordinate-specific door objects and maps their faces and directions when toggling between "Open" and "Closed" states.  
  *Editing*: Add new doors here if a door in the map does not open/close correctly.

### `/content/combat/`
- **`projectiles.toml`**  
  *Format*: TOML Array of `[[projectile]]`  
  *Purpose*: Configurations for ranged/magic spell GFX combat projectiles, including height offsets, speeds, delays, and launch slopes.  
  *Editing*: Edit this file to customize projectile physics and combat GFX behavior.

### `/content/mappings/`
- **`*.rscm`**
  *Format*: Plain-text `key=id` reference sheets (interfaces, components, objects, npcs, sequences, spot anims, varps, varbits).
  *Purpose*: Revision-pinned cache references for IntelliJ tooling and name-based search. They are never loaded by the server or used for gameplay routing; plugins own reviewed raw IDs. JSONL metadata beside the sheets preserves decoded cache fields and the provenance of resolved names.
  *Maintenance*: Regenerated programmatically using `RscmGenerator.kt`. Avoid hand-editing; regenerate and commit instead.

---

## 2. Data Directory (`/game-server/data/`)
**Git Status**: Gitignored (Untracked)  
**Access**: Engine-level reference assets loaded at startup. Should **never** be manually edited.

### `/data/cache/`
- **`main_file_cache.dat` & `main_file_cache.idx*`**  
  *Format*: OSRS Cache Binary  
  *Purpose*: Holds the binary client/server game map, models, textures, animations, and configurations.  
  *Maintenance*: Regenerated or loaded as-is using cache tools. Never edit.

### `/data/def/`
- **`items-json/`**  
  *Format*: 27,000+ individual JSON files  
  *Purpose*: Automatically generated raw Wiki scraped database of items. Used by the server as a fallback when properties are missing in `/content/items/item_definitions.json`.  
  *Maintenance*: Downloaded/updated programmatically. Never edit.
- **`cache-manifest.json`**  
  *Format*: JSON Manifest  
  *Purpose*: Cache hash verification list.

#
