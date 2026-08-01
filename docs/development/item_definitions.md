# Item Definitions System

This document explains how item definitions and metadata are loaded, merged, and managed within Dodian Ub3r (`ItemManager.kt`).

---

## Overview

The item system uses a **two-tier hybrid JSON loading architecture**:

1. **Base Item Catalog (`item_definitions.json`)**:
   - **Location**: `game-server/definitions/items/item_definitions.json`
   - **Purpose**: Defines baseline properties for all ~28,000+ OSRS item IDs.
   - **Fields**: Basic item attributes including `id`, `name`, `cost`, `bonuses` array, `slot`, and default combat animations.

2. **Per-Item Granular Overrides (`definitions/items/items-json/`)**:
   - **Location**: `game-server/definitions/items/items-json/<id>.json` (e.g., `4151.json` for Abyssal Whip).
   - **Purpose**: Provides detailed, item-specific overrides and OSRS metadata.
   - **Fields**:
     - `equipment`: Slot classification (`WEAPON`, `CHEST`, `LEGS`, etc.) and combat stat bonus arrays.
     - `cost`: Shop buy/sell values.
     - `stackable`, `noted`, `noteable`: Currency and note state flags.
     - `effectiveUnnotedId`, `effectiveNotedId`: Bidirectional item note links.

3. **Equipment Appearance Animations (`equipment_appearance.toml`)**:
   - **Location**: `game-server/definitions/items/equipment_appearance.toml`
   - **Purpose**: Defines render stance animations (stand, walk, run, attack, block) based on equipment categories (swords, whips, spears, bows, two-handers).

---

## How Loading & Merging Works

When `ItemManager.kt` initializes during server startup:

1. **Phase 1 — Base Parsing**: Loads `item_definitions.json` into a primary base catalog map (`baseMap`).
2. **Phase 2 — Appearance Rules**: Parses `equipment_appearance.toml` to associate stance and walk/run animation IDs.
3. **Phase 3 — Granular JSON Overrides**: Scans `game-server/definitions/items/items-json/` for individual `<id>.json` files:
   - If an item exists in `baseMap`, its properties (bonuses, cost, stackable status, noted linkages) are enriched by the single-item JSON.
   - If an item is missing from `baseMap`, a new `Item` instance is created directly from the single-item JSON.
4. **Phase 4 — High-Performance Array Allocation**: All items are indexed into a fast primitive `Item[]` array for zero-overhead index lookups (`Server.itemManager.items[itemId]`).

---

## Creating or Modifying an Item

### Modifying an Existing Item
To update stats or attributes for a specific item (e.g. Abyssal Whip `4151`):
1. Open `game-server/definitions/items/items-json/4151.json` (or create it if it doesn't exist).
2. Modify cost, equipment bonuses, or note linkages.
3. Restart the server or invoke dev command reload.

### Item JSON Format Example (`4151.json`)
```json
{
  "id": 4151,
  "name": "Abyssal whip",
  "cost": 120000,
  "stackable": false,
  "noted": false,
  "noteable": true,
  "effectiveUnnotedId": 4151,
  "effectiveNotedId": 4152,
  "equipment": {
    "slot": "WEAPON",
    "bonuses": [0, 82, 0, 0, 0, 0, 0, 0, 0, 0, 82, 0, 0, 0]
  }
}
```
