package net.dodian.uber.game.api.plugin.skills

import net.dodian.uber.game.api.content.ContentAttributes
import net.dodian.uber.game.engine.tasking.TaskPriority
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.skill.runtime.action.ActionStopReason
import net.dodian.uber.skills.api.SkillMultiConfig
import net.dodian.uber.skills.api.SkillMultiSelection

data class SkillPosition(val x: Int, val y: Int, val z: Int = 0)

data class SkillObjectRef(
    val id: Int,
    val position: SkillPosition,
    val type: Int = 10,
    val face: Int = 0,
    val sizeX: Int = 1,
    val sizeY: Int = 1,
)

data class SkillNpcRef(val id: Int, val index: Int, val position: SkillPosition)

interface SkillPlayer {
    val skills: SkillLevels
    val inventory: SkillInventory
    val equipment: SkillEquipment
    val actions: SkillActions
    val ui: SkillUi
    val world: SkillWorld
    val production: SkillProduction
    val amountPrompt: SkillAmountPrompt
    val profile: SkillProfile
    val random: SkillRandom
    val clock: SkillClock
    val vitals: SkillVitals
    val runePouches: SkillRunePouches
    val slayerTask: SkillSlayerTask
    /** Plugin-owned temporary state; cleared when the player logs out. */
    val attributes: ContentAttributes
    val moduleState: SkillModuleState
}

interface SkillLevels {
    fun current(skill: Skill): Int
    fun base(skill: Skill): Int
    fun experience(skill: Skill): Int
    fun gainXp(amount: Int, skill: Skill): Boolean
}

/** Module-keyed transient state. Values are scoped to the declaring module and cleared on logout. */
interface SkillModuleState {
    fun get(moduleId: String, key: String): String?
    fun put(moduleId: String, key: String, value: String)
    fun remove(moduleId: String, key: String)
}

interface SkillInventory {
    fun contains(itemId: Int, amount: Int = 1): Boolean
    fun amount(itemId: Int): Int
    fun slotAmount(slot: Int, itemId: Int): Int
    fun freeSlots(): Int
    fun add(itemId: Int, amount: Int = 1): Boolean
    fun remove(itemId: Int, amount: Int = 1): Boolean
    fun transaction(block: SkillInventoryTransaction.() -> Unit): Boolean
    fun itemName(itemId: Int): String
    fun notedItemId(itemId: Int): Int
    fun refresh()
}

interface SkillInventoryTransaction {
    fun require(itemId: Int, amount: Int = 1): Boolean
    fun remove(itemId: Int, amount: Int = 1): Boolean
    fun removeAt(slot: Int, itemId: Int, amount: Int = 1): Boolean
    fun add(itemId: Int, amount: Int = 1): Boolean
}

interface SkillEquipment {
    fun item(slot: Int): Int
    fun amount(slot: Int): Int
    fun remove(slot: Int, itemId: Int, amount: Int = 1): Boolean
    fun refresh()
}

object SkillEquipmentSlot { const val HEAD = 0; const val WEAPON = 3 }

fun interface SkillRequirement {
    fun validate(player: SkillPlayer): SkillValidationResult
    fun execute(player: SkillPlayer) {}
}

sealed class SkillValidationResult {
    object Ok : SkillValidationResult()
    data class Failed(val message: String) : SkillValidationResult()
    companion object {
        @JvmStatic fun ok(): SkillValidationResult = Ok
        @JvmStatic fun failed(message: String): SkillValidationResult = Failed(message)
    }
}

data class SkillActionSpec(
    val name: String,
    val delayCalculator: SkillPlayer.() -> Int,
    val requirements: List<SkillRequirement>,
    val priority: TaskPriority,
    val onStart: (SkillPlayer.() -> Unit)?,
    val onCycle: (SkillPlayer.() -> SkillCycleSignal)?,
    val onSuccess: (SkillPlayer.() -> Unit)?,
    val onStop: (SkillPlayer.(ActionStopReason) -> Unit)?,
)

data class SkillCycleSignal(
    val keepRunning: Boolean,
    val succeeded: Boolean = true,
    val stopReason: ActionStopReason? = null,
) {
    companion object {
        fun success() = SkillCycleSignal(true, true)
        fun continueWithoutSuccess() = SkillCycleSignal(true, false)
        fun completeSuccess() = SkillCycleSignal(false, true, ActionStopReason.COMPLETED)
        fun stop(reason: ActionStopReason? = null) = SkillCycleSignal(false, false, reason)
    }
}

interface SkillActionHandle { fun cancel(reason: ActionStopReason = ActionStopReason.USER_INTERRUPT) }

interface SkillActions {
    fun animate(id: Int, delay: Int = 0)
    fun queue(spec: SkillActionSpec, beforeStart: () -> Unit = {}): SkillActionHandle?
    fun stop()
    fun lockMovement(locked: Boolean)
    fun beginSession(key: String): Boolean
    fun endSession(key: String)
    fun activeSessionKey(): String?
    fun triggerRandomEvent(experience: Int)
    /** Records an audited resource-gathering event (fishing/mining/woodcutting/thieving loot, etc.). */
    fun logGathering(itemId: Int, amount: Int, reason: String)
    /** Broadcasts [message] to every online player (e.g. a rare-drop announcement). */
    fun yell(message: String)
    /**
     * Per-player, per-[key] cooldown gate: returns `true` (and starts the cooldown) only if at
     * least [cooldownMs] has elapsed since the last successful acquire for this [key] on this
     * player. Use a module-unique key (e.g. `"thieving.yanille_chest"`) to throttle a specific
     * interaction without needing engine-side constants.
     */
    fun tryThrottle(key: String, cooldownMs: Long): Boolean
}

interface SkillUi {
    fun message(text: String)
    fun string(text: String, componentId: Int)
    fun open(interfaceId: Int)
    fun close()
    fun chatbox(interfaceId: Int)
    fun itemModel(componentId: Int, zoom: Int, itemId: Int)
    /** Renders an item/amount grid into a legacy component without exposing packets to content modules. */
    fun itemGrid(componentId: Int, entries: List<SkillItemGridEntry>)
    fun componentVisible(componentId: Int, visible: Boolean)
    fun menuGrid(entries: List<SkillItemGridEntry>)
    fun npcDialogue(dialogueId: Int, npcId: Int)
    fun varbit(id: Int, value: Int)
    /** Opens an existing client skill-guide page without exposing Client to plugin content. */
    fun skillGuide(skillId: Int, tab: Int = 0)
    /** Opens the guide-book item page. */
    fun guideBook()
}

data class SkillItemGridEntry(val itemId: Int, val amount: Int)

interface SkillWorld {
    val position: SkillPosition
    fun distanceTo(x: Int, y: Int): Int
    fun teleport(destination: SkillPosition)
    /** Restores the player to the engine's configured safe/default position. */
    fun resetPosition()
    fun anchor(position: SkillPosition)
    fun face(position: SkillPosition)
    /** Plays graphic [id] at the player's own position. */
    fun graphic(id: Int, height: Int = 0)
    /** Plays graphic [id] at an arbitrary world [position] (e.g. on the object being interacted with). */
    fun graphic(id: Int, position: SkillPosition, height: Int = 0)
    /**
     * Swaps [target] for [replacementId] world-wide (every nearby viewer sees it, not just this
     * player), reverting back to [target]'s original id after [restoreTicks] ticks (0 = permanent
     * until something else replaces it). [type]/[face] set the replacement object's orientation;
     * they default to [target]'s own values when the swap doesn't need to change orientation.
     */
    fun replaceObject(
        target: SkillObjectRef,
        replacementId: Int,
        restoreTicks: Int = 0,
        type: Int = target.type,
        face: Int = target.face,
    )
    /** Whether the player is still within interaction range of [target]'s nearest boundary tile (accounts for object size/shape, not just Chebyshev tile distance). */
    fun withinObjectBoundary(target: SkillObjectRef): Boolean
    /** Spawns [objectId] at the player's current position for [durationTicks] ticks, then removes it and invokes [onExpire]. */
    fun spawnTemporaryObject(objectId: Int, durationTicks: Int, onExpire: () -> Unit = {})
    /** Drops [amount] of [itemId] on the ground at the player's current position, initially visible only to them. */
    fun dropItem(itemId: Int, amount: Int = 1)
    /** Drops [amount] of [itemId] on the ground at [position], initially visible only to them. */
    fun dropItemAt(position: SkillPosition, itemId: Int, amount: Int = 1)
    /**
     * Animates a scripted traversal (e.g. crossing an agility obstacle): walks a relative offset
     * over [durationMs] using [movementAnimationId] as the walk animation (when given), optionally
     * playing [startAnimationId] immediately and [arrivalAnimationId] on arrival, then invokes
     * [onComplete]. Grants temporary passage across the straight-line path so normally-blocked
     * tiles can be crossed.
     */
    fun traverse(
        deltaX: Int,
        deltaY: Int,
        durationMs: Int,
        movementAnimationId: Int? = null,
        startAnimationId: Int? = null,
        arrivalAnimationId: Int? = null,
        onComplete: () -> Unit = {},
    )
    /**
     * Plays [animationId] immediately, then after [delayMs] teleports to [destination] and
     * invokes [onComplete] (e.g. climbing over/through an obstacle rather than walking across it).
     */
    fun climb(destination: SkillPosition, animationId: Int, delayMs: Int, onComplete: () -> Unit = {})
}

/** View of the player's persisted slayer assignment. */
interface SkillSlayerTask {
    /** NPC id of the slayer master who assigned the current task, or -1 if none. */
    val masterNpcId: Int
    /** Persisted task-table ordinal, or -1 when no task is assigned. */
    val ordinal: Int
    val assignmentAmount: Int
    /** Display name of the assigned task, or an empty string when no task is active. */
    val taskName: String
    val remainingAmount: Int
    val streak: Int
    /** Kills logged against the assigned task's monsters this task, or -1 if no task is active. */
    fun killCount(): Int
    /** Resets the assigned task's kill counter. Returns false if there's no active task. */
    fun resetKillCount(): Boolean
}

interface SkillProduction {
    fun open(config: SkillMultiConfig, onSelected: (SkillMultiSelection) -> Unit): Boolean
    fun select(selection: SkillMultiSelection): Boolean
    fun pending(): SkillMultiConfig?
    fun clear()
}

/**
 * A protocol-free numeric input request. The engine keeps the callback only
 * for the current player session and routes the legacy syntax packet back to
 * it. Content must not retain a prompt after requesting it.
 */
interface SkillAmountPrompt {
    fun request(title: String = "Enter amount:", maxDigits: Int = 10, onEntered: (Int) -> Boolean): Boolean
    fun cancel()
}

interface SkillProfile { val name: String; val premium: Boolean; val combatLevel: Int }
interface SkillRandom { fun between(minInclusive: Int, maxInclusive: Int): Int; fun chance(numerator: Int, denominator: Int): Boolean }
/** Deterministic module clock; adapters map this to the game/server clock. */
fun interface SkillClock { fun nowMillis(): Long }
interface SkillVitals {
    val currentPrayer: Int
    val maximumPrayer: Int
    /**
     * Whether a prayer is currently active.  Skill modules use this stable
     * identifier instead of reaching into the engine's prayer manager.
     */
    fun isPrayerActive(prayer: SkillPrayer): Boolean
    /** Toggles a prayer while preserving the engine's level, drain, and UI rules. */
    fun togglePrayer(prayer: SkillPrayer)
    fun deactivatePrayer(prayer: SkillPrayer)
    fun setPrayer(amount: Int)
    fun damage(amount: Int)
    fun restorePrayer(amount: Int)
    fun stun(ticks: Int)
}

/** Persisted rune-pouch state exposed to the Runecrafting plugin only. */
interface SkillRunePouches {
    fun amount(slot: Int): Int
    fun addAmount(slot: Int, amount: Int)
    fun levelRequirement(slot: Int): Int
    fun capacity(slot: Int): Int
}

/** Protocol- and engine-independent prayer identifiers used by skill modules. */
enum class SkillPrayer {
    THICK_SKIN,
    BURST_OF_STRENGTH,
    CLARITY_OF_THOUGHT,
    SHARP_EYE,
    MYSTIC_WILL,
    ROCK_SKIN,
    SUPERHUMAN_STRENGTH,
    IMPROVED_REFLEXES,
    HAWK_EYE,
    MYSTIC_LORE,
    RAPID_RESTORE,
    RAPID_HEAL,
    PROTECT_ITEM,
    STEEL_SKIN,
    ULTIMATE_STRENGTH,
    INCREDIBLE_REFLEXES,
    EAGLE_EYE,
    MYSTIC_MIGHT,
    PROTECT_MAGIC,
    PROTECT_RANGE,
    PROTECT_MELEE,
    RETRIBUTION,
    REDEMPTION,
    SMITE,
    CHIVALRY,
    PIETY,
    ;

    /** The legacy client button id; protocol details stay data-only at the module boundary. */
    val buttonId: Int
        get() = BUTTON_IDS[ordinal]

    private companion object {
        val BUTTON_IDS = intArrayOf(
            5609, 5610, 5611, 19812, 19814, 5612, 5613, 5614, 19816, 19818,
            5615, 5616, 5617, 5618, 5619, 5620, 19821, 19823, 5621, 5622, 5623,
            683, 684, 685, 19825, 19827,
        )
    }
}

class SkillObjectInteraction(val player: SkillPlayer, val option: Int, val target: SkillObjectRef) {
    val objectId get() = target.id
    val position get() = target.position
    val definition get() = target
}
class SkillNpcInteraction(val player: SkillPlayer, val option: Int, val npc: SkillNpcRef)
class SkillItemOnItemInteraction(val player: SkillPlayer, val itemUsed: Int, val otherItem: Int)
class SkillItemInteraction(val player: SkillPlayer, val option: Int, val itemId: Int, val itemSlot: Int, val interfaceId: Int)
class SkillItemOnObjectInteraction(val player: SkillPlayer, val target: SkillObjectRef, val itemId: Int, val itemSlot: Int, val interfaceId: Int) {
    val objectId get() = target.id
    val position get() = target.position
    val definition get() = target
}
class SkillMagicOnObjectInteraction(val player: SkillPlayer, val target: SkillObjectRef, val spellId: Int) {
    val objectId get() = target.id
    val position get() = target.position
    val definition get() = target
}
class SkillButtonInteraction(val player: SkillPlayer, val rawButtonId: Int, val opIndex: Int, val activeInterfaceId: Int)
class SkillItemGridInteraction(val player: SkillPlayer, val interfaceId: Int, val itemId: Int, val slot: Int, val amount: Int)
