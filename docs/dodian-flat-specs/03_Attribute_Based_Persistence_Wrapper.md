# Spec 03: Attribute-Based Persistence Wrapper
### Dodian-Flat Final Draft — Based on `game-server old` Actual Codebase Audit

---

> ⛔ **READ [00_MASTER_RULES_READ_FIRST.md](./00_MASTER_RULES_READ_FIRST.md) BEFORE THIS SPEC.**
> The `AttributeMap` and `AttributeKey` system defined here is a **facade over existing MySQL columns**. No new columns, no new tables, no new player data beyond what the `effects` overflow bucket provides.
> All `AttributeKey` constants you create must map to a field that **already exists** in `Client.java` or the `characters`/`character_stats` tables. Creating a key for data that doesn't currently exist is only allowed if it uses the `EffectsBlob` overflow system.

---

## 1. Executive Summary

The player persistence system in `game-server old` is already substantially modernized in Kotlin. The `PlayerSaveSqlRepository` at `net.dodian.uber.game.persistence.player.PlayerSaveSqlRepository` handles all SQL encoding for the `characters` and `character_stats` MySQL tables. It is **segment-based**: the `PlayerSaveEnvelope` is composed of typed `PlayerSaveSegmentSnapshot` objects (`StatsSegmentSnapshot`, `InventorySegmentSnapshot`, `BankSegmentSnapshot`, etc.) that are populated from the `Client` Java object and then encoded to SQL update strings at save time.

The constraint: **zero MySQL schema changes are permitted.** The `characters` and `character_stats` tables are fixed. The `effects` TEXT column is available as a JSON overflow bucket for transient or new variables that have no dedicated column.

This spec defines the `AttributeMap` facade: an in-memory, typed, per-player map of keys to values, each key bound to a `PersistenceBridge` that knows how to read and write its value from the existing `Client` object fields and the existing save segments. Content developers use only the `AttributeMap` — they never touch `Client` fields directly, never touch `PlayerSaveSqlRepository`, and never write SQL.

---

## 2. Infrastructure: Where This Lives

The attribute system lives in `net.dodian.uber.game.systems.attr`. This package does not yet exist — it must be created. It is a **systems-layer** package, meaning it wraps engine infrastructure (the persistence layer) and provides a clean API to content.

```
net.dodian.uber.game.systems.attr/
    AttributeKey.kt          — Typed key definition with optional PersistenceBridge
    AttributeBridge.kt       — Interface for read/write to Client fields
    AttributeMap.kt          — The per-player in-memory map
    AttributeRegistry.kt     — Singleton catalog of all defined keys
    PlayerAttributes.kt      — All concrete AttributeKey definitions for existing DB columns
    EffectsBlob.kt           — JSON serialization for overflow data in the `effects` column
```

---

## 3. Core Types

### 3.1 `AttributeKey<T>` — Strongly Typed Key

```kotlin
// net.dodian.uber.game.systems.attr.AttributeKey
package net.dodian.uber.game.systems.attr

/**
 * A typed key for the attribute system.
 *
 * @param name          Unique string identifier used in debugging and the effects JSON blob.
 * @param defaultValue  Value returned when the attribute has not yet been set.
 * @param bridge        Optional bridge to a legacy Client field or save segment.
 *                      If null, the value is transient (in-memory only, not persisted).
 */
class AttributeKey<T : Any>(
    val name: String,
    val defaultValue: T,
    val bridge: AttributeBridge<T>? = null,
)
```

### 3.2 `AttributeBridge<T>` — The Legacy Bridge Interface

```kotlin
// net.dodian.uber.game.systems.attr.AttributeBridge
package net.dodian.uber.game.systems.attr

import net.dodian.uber.game.model.entity.player.Client

/**
 * Bidirectional bridge between an AttributeKey and a legacy Client field.
 * Implementations read/write directly to the field on the Java Client object.
 * These bridges must NEVER be called from the content layer directly.
 */
interface AttributeBridge<T : Any> {
    fun get(client: Client): T
    fun set(client: Client, value: T)
}
```

### 3.3 `AttributeMap` — The Per-Player Store

```kotlin
// net.dodian.uber.game.systems.attr.AttributeMap
package net.dodian.uber.game.systems.attr

import net.dodian.uber.game.model.entity.player.Client

/**
 * Per-player in-memory attribute store.
 * On first read of a bridged key, the value is hydrated from the client field.
 * On write of a bridged key, the client field is updated immediately.
 *
 * This map is attached to the Client on login and cleared on logout.
 */
class AttributeMap(private val client: Client) {
    private val store = HashMap<AttributeKey<*>, Any?>()

    @Suppress("UNCHECKED_CAST")
    operator fun <T : Any> get(key: AttributeKey<T>): T {
        val cached = store[key]
        if (cached != null) return cached as T
        // Hydrate from bridge on first access
        val bridge = key.bridge
        val value = if (bridge != null) {
            bridge.get(client).also { store[key] = it }
        } else {
            key.defaultValue.also { store[key] = it }
        }
        return value
    }

    operator fun <T : Any> set(key: AttributeKey<T>, value: T) {
        store[key] = value
        // Write through to legacy field immediately
        key.bridge?.set(client, value)
    }

    fun <T : Any> getOrDefault(key: AttributeKey<T>): T = this[key]

    /**
     * Used by the persistence system when building a PlayerSaveEnvelope.
     * Returns all attributes that have been touched in this map.
     */
    fun touchedEntries(): Map<AttributeKey<*>, Any?> = store.toMap()

    fun clear() {
        store.clear()
    }
}
```

---

## 4. The Complete Legacy Bridge Map

Every column in the `characters` and `character_stats` tables maps to an `AttributeKey` with a `PersistenceBridge`. All bridge implementations are defined in `PlayerAttributes.kt`. Content developers use only the key — they never call the bridge method directly.

### 4.1 `characters` Table Column Map

These column names match exactly what `PlayerSaveSqlRepository.buildCharacterQuery()` writes.

| AttributeKey Constant | Client Field | SQL Column | Type | Notes |
|:---|:---|:---|:---|:---|
| `STAFF_RIGHTS` | `client.rights` | `rights` | `INT` | Player right/rank level |
| `MEMBERSHIP` | `client.mgroup` | `mgroup` | `INT` | Membership group |
| `TOTAL_KILLS` | `client.kc` | `kc` | `INT` | Kill count |
| `TOTAL_DEATHS` | `client.dc` | `dc` | `INT` | Death count |
| `FIGHT_STYLE` | `client.fightStyle` | `fightStyle` | `INT` | Written as `stats.fightType` in save |
| `HEALTH` | `client.health` | `health` | `INT` | Written as `stats.currentHealth` |
| `HEIGHT` | `client.height` | `height` | `INT` | Plane/floor |
| `POS_X` | `client.absX` | `x` | `INT` | Written by `PositionSegmentSnapshot` |
| `POS_Y` | `client.absY` | `y` | `INT` | Written by `PositionSegmentSnapshot` |
| `PK_RATING` | `client.pkrating` | `pkrating` | `INT` | Always saved as 1500 currently |
| `LAST_LOGIN` | `client.lastlogin` | `lastlogin` | `LONG` | Set to `System.currentTimeMillis()` on save |
| `GENDER` | `client.pGender` | `gender` | `INT` | 0=male, 1=female |
| `HEAD_ICON` | `client.headIcon` | `headIcon` | `INT` | Prayer head icon |
| `PRAYER_STATE` | `client.prayer` | `prayer` | `TEXT` | Encoded as `currentPrayer:btn1:btn2` |
| `BOOSTED_LEVELS` | `client.boosted` | `boosted` | `TEXT` | Encoded as `lastRecover:boost0:boost1:...` |
| `EQUIPMENT` | `client.equipment` | `equipment` | `TEXT` | Encoded as `slot-id-amt slot-id-amt ...` |
| `INVENTORY` | `client.inventory` | `inventory` | `TEXT` | Same encoding |
| `BANK` | `client.bank` | `bank` | `TEXT` | Same encoding |
| `FRIENDS` | `client.friends` | `friends` | `TEXT` | Space-separated names |
| `SLAYER_DATA` | `client.slayerData` | `slayerData` | `TEXT` | Custom slayer encoding |
| `POUCH_DATA` | `client.essence` | `essence_pouch` | `TEXT` | Rune essence pouch contents |
| `AUTOCAST_ID` | `client.autocast` | `autocast` | `INT` | Autocast spell index |
| `EFFECTS_BLOB` | `client.effects` | `effects` | `TEXT` | JSON overflow for new variables |
| `FARMING_DATA` | `client.farming` | `farming` | `TEXT` | Farming patch state |
| `DAILY_REWARD` | `client.dailyReward` | `dailyReward` | `TEXT` | Daily reward tracking |
| `SONGS_UNLOCKED` | `client.songUnlocked` | `songUnlocked` | `TEXT` | Music unlock bitfield |
| `TRAVEL_DATA` | `client.travel` | `travel` | `TEXT` | Travel unlock data |
| `LOOK_DATA` | `client.look` | `look` | `TEXT` | Appearance data |
| `UNLOCKS_DATA` | `client.unlocks` | `unlocks` | `TEXT` | Feature unlock flags |
| `NEWS_ID` | `client.news` | `news` | `INT` | Latest news ID seen |
| `AGILITY_STAGE` | `client.agility` | `agility` | `TEXT` | Agility course state |
| `MONSTER_LOG` | `client.monLog` | `Monster_Log` | `TEXT` | Monster kill log (name,count pairs) |
| `BOSS_LOG` | `client.bossLog` | `Boss_Log` | `TEXT` | Boss kill log (name:count pairs) |

### 4.2 `character_stats` Table Column Map

These columns are written by the stats query in `PlayerSaveSqlRepository.buildSnapshot()`. Each skill has its own column named after the skill in lowercase (e.g., `attack`, `defence`, `strength`).

| AttributeKey Constant | Skill ID | SQL Column | Persistence Rule |
|:---|:---|:---|:---|
| `ATTACK_XP` | 0 | `attack` | Written on every save |
| `DEFENCE_XP` | 1 | `defence` | Written on every save |
| `STRENGTH_XP` | 2 | `strength` | Written on every save |
| `HITPOINTS_XP` | 3 | `hitpoints` | Written on every save |
| `RANGED_XP` | 4 | `ranged` | Written on every save |
| `PRAYER_XP` | 5 | `prayer` | Written on every save |
| `MAGIC_XP` | 6 | `magic` | Written on every save |
| `COOKING_XP` | 7 | `cooking` | Written on every save |
| `WOODCUTTING_XP` | 8 | `woodcutting` | Written on every save |
| `FLETCHING_XP` | 9 | `fletching` | Written on every save |
| `FISHING_XP` | 10 | `fishing` | Written on every save |
| `FIREMAKING_XP` | 11 | `firemaking` | Written on every save |
| `CRAFTING_XP` | 12 | `crafting` | Written on every save |
| `SMITHING_XP` | 13 | `smithing` | Written on every save |
| `MINING_XP` | 14 | `mining` | Written on every save |
| `HERBLORE_XP` | 15 | `herblore` | Written on every save |
| `AGILITY_XP` | 16 | `agility` | Written on every save |
| `THIEVING_XP` | 17 | `thieving` | Written on every save |
| `SLAYER_XP` | 18 | `slayer` | Written on every save |
| `FARMING_XP` | 19 | `farming` | Written on every save |
| `RUNECRAFT_XP` | 20 | `runecraft` | Written on every save |
| `TOTAL_LEVEL` | N/A | `total` | Calculated: sum of levels |
| `COMBAT_LEVEL` | N/A | `combat` | Calculated via combat formula |
| `TOTAL_XP` | N/A | `totalxp` | Sum of all XP |

The `Skill.values()` enum iteration in `PlayerSaveSqlRepository` uses `Skill.name.lowercase()` as the SQL column name. The `Skill` enum in `net.dodian.uber.game.model.player.skills.Skill` is the authoritative list of enabled skills.

---

## 5. The `EFFECTS_BLOB` Overflow System

The `effects` column is a TEXT column currently used to store active effect/buff status. Its current encoding is a colon-separated list of integer effect IDs (read from `client.effects`, saved as `effects.effects.joinToString(":")`).

For new per-player variables that have no dedicated column, we use the `EFFECTS_BLOB` as a JSON container. The approach:

1. The `effects` column stores two sections, separated by a vertical bar `|`:
   - Section 1: Legacy colon-separated integers (unchanged — do not break existing effect IDs)
   - Section 2: Base64-encoded JSON blob for new key-value overflows

2. At load time, the `EffectsBlob` parser splits on `|`, reads legacy effects from section 1, and deserializes `Map<String, String>` from section 2.

3. At save time, the `EffectsBlob` serializes the overflow map to JSON, base64-encodes it, and appends it after `|` before writing to the `effects` column.

> **Constraint:** If no overflow data exists, nothing is appended (no `|` separator), so existing players load correctly without any data migration.

### 5.1 `EffectsBlob` Implementation

```kotlin
// net.dodian.uber.game.systems.attr.EffectsBlob
package net.dodian.uber.game.systems.attr

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.util.Base64

/**
 * Manages the JSON overflow section of the `effects` database column.
 * The column format is: "legacy_effect_1:legacy_effect_2|BASE64_JSON_OVERFLOW"
 * If no overflow, the column is just: "legacy_effect_1:legacy_effect_2"
 */
object EffectsBlob {
    private val mapper = jacksonObjectMapper()

    fun parseOverflow(raw: String): Map<String, String> {
        val pipe = raw.indexOf('|')
        if (pipe < 0) return emptyMap()
        val encoded = raw.substring(pipe + 1)
        if (encoded.isBlank()) return emptyMap()
        return try {
            val json = Base64.getDecoder().decode(encoded).toString(Charsets.UTF_8)
            mapper.readValue(json)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun encodeLegacyWithOverflow(legacyPart: String, overflow: Map<String, String>): String {
        if (overflow.isEmpty()) return legacyPart
        val json = mapper.writeValueAsString(overflow)
        val encoded = Base64.getEncoder().encodeToString(json.toByteArray(Charsets.UTF_8))
        return "$legacyPart|$encoded"
    }
}
```

### 5.2 Using the Overflow System for New Variables

Content developers define a new `AttributeKey` with a bridge that reads/writes to the overflow map:

```kotlin
// Example: a new per-player variable "bounty_points" with no dedicated column
val BOUNTY_POINTS = AttributeKey(
    name = "bounty_points",
    defaultValue = 0,
    bridge = EffectsOverrideBridge("bounty_points", 0) { it.toInt() } { it.toString() }
)
```

The `EffectsOverrideBridge` class handles all encoding/decoding against the overflow map, which is then folded back into the `effects` column on save via the existing `EffectsSegmentSnapshot`.

---

## 6. Integration with the Existing Save System

The `AttributeMap` does **not** replace the save system — it wraps it. The existing save pipeline (`PlayerSaveService` → `PlayerSaveSqlRepository` → `PlayerSaveRepository`) remains unchanged. The `AttributeMap` provides:

1. **Read-through hydration:** When content reads `player.attr[TOTAL_KILLS]`, the bridge reads `client.kc` once and caches it.
2. **Write-through sync:** When content sets `player.attr[TOTAL_KILLS] = newValue`, the bridge immediately sets `client.kc = newValue`. The existing save system then reads `client.kc` when building the segment snapshot.

This means the `PlayerSaveSqlRepository` never needs to be modified. The segment snapshots continue to read directly from `Client` fields — and those fields are always in sync because the bridge updates them.

### 6.1 Attaching the `AttributeMap` to the Player

The `AttributeMap` must be attached to the `Client` object (or a Kotlin extension on it) at login and cleared at logout. Proposed approach:

```kotlin
// Extension on Client — in systems.attr package
private val attributeMaps = WeakHashMap<Client, AttributeMap>()

val Client.attr: AttributeMap
    get() = attributeMaps.getOrPut(this) { AttributeMap(this) }

fun Client.clearAttributes() {
    attributeMaps.remove(this)
}
```

Using a `WeakHashMap` ensures the `AttributeMap` is garbage collected if the `Client` object is GC'd (e.g., after logout). `clearAttributes()` should be called explicitly from the logout handler for clean teardown.

---

## 7. Content Developer Experience

After the `AttributeMap` is in place, content code looks like this:

```kotlin
// Before: direct Client field access (forbidden in new content)
if (player.kc > 100) {
    player.sendMessage("You're a veteran killer!")
}

// After: attribute map access (correct pattern)
if (player.attr[PlayerAttributes.TOTAL_KILLS] > 100) {
    player.sendMessage("You're a veteran killer!")
}

// Writing:
player.attr[PlayerAttributes.TOTAL_KILLS] = player.attr[PlayerAttributes.TOTAL_KILLS] + 1
```

For skill XP, content developers use the `ProgressionService` (Spec 07), not the attribute map directly. The attribute map skill keys are used internally by the persistence bridge. A content developer adding XP should call:
```kotlin
ProgressionService.addXp(player, Skills.MINING, 17.5)
```
This internally updates the skill data and the attribute map updates the `Client` field.

---

## 8. Transient Attributes (Non-Persisted)

Some per-player variables are session-only and should not persist across logins. These use an `AttributeKey` with `bridge = null`:

```kotlin
// Transient: cleared when player logs out, never written to DB
val CURRENT_SKILLING_NODE = AttributeKey<Int?>(
    name = "current_skilling_node",
    defaultValue = null,
    bridge = null,
)

val IS_IN_CUTSCENE = AttributeKey(
    name = "is_in_cutscene",
    defaultValue = false,
    bridge = null,
)
```

---

## 9. Pathfinding State Attributes

Movement state (`pendingInteraction`, `interactionEarliestCycle`, `wQueueReadPtr`, etc.) MUST NOT be wrapped in `AttributeKey` with persistence bridges. These are purely transient engine state fields on `Client.java` and will be managed by the future pathfinding system. Do not add bridge keys for anything related to walk queues or interaction intents.

---

## 10. Definition of Done for Spec 03

- [ ] `net.dodian.uber.game.systems.attr` package created
- [ ] `AttributeKey<T>` class created with `name`, `defaultValue`, `bridge`
- [ ] `AttributeBridge<T>` interface created with `get(client)` and `set(client, value)`
- [ ] `AttributeMap` class created with `get`, `set`, `getOrDefault`, `touchedEntries`, `clear`
- [ ] `AttributeRegistry` singleton lists all defined keys (for debugging and documentation)
- [ ] `PlayerAttributes.kt` defines `AttributeKey` constants for every `characters` column listed in Section 4.1
- [ ] `PlayerAttributes.kt` defines `AttributeKey` constants for every `character_stats` skill column listed in Section 4.2
- [ ] `EffectsBlob` implemented for JSON overflow in the `effects` column
- [ ] `Client.attr` extension property attaches `AttributeMap` using `WeakHashMap`
- [ ] `Client.clearAttributes()` extension called from logout handler
- [ ] No `AttributeKey` bridge wraps walk-queue or pathfinding state
- [ ] The `PlayerSaveSqlRepository` is UNCHANGED — bridges write through to Client fields, save system reads Client fields as before
- [ ] MySQL schema is UNCHANGED
- [ ] All `character_stats` skill bridges correctly map to the `Skill` enum ordering
- [ ] `EffectsBlob` reads legacy colon-separated values without corruption when no JSON overflow section exists
- [ ] Content in `content.*` packages does not import `PlayerSaveSqlRepository`, `PlayerSaveService`, or anything from `persistence.*`
