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
    val farmingState: SkillFarmingState
    /** Plugin-owned temporary state; cleared when the player logs out. */
    val attributes: ContentAttributes
    val moduleState: SkillModuleState
}

/** A single patch's persisted state - see [SkillFarmingState.patchSlots]. */
data class SkillPatchSlot(
    val patchName: String,
    val slot: Int,
    val itemId: Int,
    val state: String,
    val compost: String,
    val stageOrLife: Int,
    val progress: Int,
    val plantedBy: Int,
)

/** A single compost bin's persisted state - see [SkillFarmingState.compostBins]. */
data class SkillCompostBinState(
    val binName: String,
    val compost: String,
    val state: String,
    val amount: Int,
    val progress: Int,
)

/**
 * Read/write access to the player's persisted farming state - a thin plugin-facing view over
 * the engine's existing `FarmingPersistenceCodec`/`client.farmingJson` storage (not a new
 * persistence mechanism; nothing else needs a generic persisted-blob API yet). `state`/
 * `compost` values are the same plain strings the legacy `FarmingData.patchState`/`.compost`
 * enums already used (e.g. `"WEED"`, `"GROWING"`, `"NONE"`) - kept as strings at this boundary
 * rather than a new shared enum type, matching how other migrated skills' data-driven string
 * constants already cross this same boundary.
 */
interface SkillFarmingState {
    fun patchSlots(): List<SkillPatchSlot>
    fun writePatchSlot(patchName: String, slot: Int, itemId: Int, state: String, compost: String, stageOrLife: Int, progress: Int, plantedBy: Int)
    fun compostBins(): List<SkillCompostBinState>
    fun writeCompostBin(binName: String, compost: String, state: String, amount: Int, progress: Int)
    /** Marks the farming save segment dirty and refreshes the cached save snapshot. */
    fun markDirty()
    /**
     * Refreshes the client's patch varbits immediately (the legacy visual-only
     * `updateFarmPatch()` refresh) - call after any state-changing interaction so the client
     * sees the new patch state right away, not just on the next login/walk-triggered refresh.
     */
    fun refreshVisuals()
    /**
     * Nudges the wall-clock growth-catch-up scheduler to apply any due pulses immediately
     * instead of waiting for its next poll - call alongside [markDirty]/[refreshVisuals] after
     * any patch or compost-bin interaction that changes state, matching the legacy
     * `ContentRuntimeApi.onFarmingPatchInteraction`/`.onFarmingCompostInteraction` call points
     * (their underlying scheduler bodies are identical, hence one consolidated method here).
     */
    fun notifyInteraction()
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

object SkillEquipmentSlot { const val HEAD = 0; const val WEAPON = 3; const val RING = 12 }

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
    /** Generic engine action lock exposed without leaking the concrete player implementation. */
    val busy: Boolean get() = false
    fun animate(id: Int, delay: Int = 0)
    fun resetAnimation() = animate(-1, 0)
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
    /**
     * Marks the shared magic-cast cooldown as consumed for this cycle (the same cooldown every
     * magic-on-item cast - alchemy, enchant, superheat - is gated on). Legacy Superheat only set
     * this after its own validation passed (level/rune/ore-presence) but before rolling for
     * success, so a rejected cast doesn't consume the cooldown while a valid-but-unlucky one
     * does; call this at the equivalent point when porting similar cast logic, not automatically
     * on dispatch.
     */
    fun markMagicCastCycle()
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
    /**
     * Opens [dialogueId] directly, with no blocking-interaction guard (unlike [npcDialogue],
     * which goes through the engine's "already mid-interaction" check). Use only when porting
     * legacy content that opened a dialogue id the same bare way - e.g. an item-on-item trigger
     * that never had that guard applied to it.
     */
    fun openDialogue(dialogueId: Int)
    fun varbit(id: Int, value: Int)
    /**
     * Opens a paginated (3-per-page, with Close/Next/Previous) picker over an arbitrary-length
     * [items] list of item ids, showing each item's name. Calls [onSelect] once with the chosen
     * item id when the player picks one - the menu does not auto-reopen after a selection (the
     * caller re-opens it explicitly, e.g. after an amount prompt completes, for a "keep picking
     * until you close" workflow). Use this instead of `playerOptionMenu`'s fixed 2-5-option
     * shape when the option count is dynamic/unbounded.
     */
    fun itemListMenu(items: List<Int>, onSelect: (Int) -> Unit)
    /** Opens an existing client skill-guide page without exposing Client to plugin content. */
    fun skillGuide(skillId: Int, tab: Int = 0)
    /** Opens the guide-book item page. */
    fun guideBook()
    /** The skill id of the currently-open skill guide page, or -1 if none is open. */
    fun currentSkillGuideSkillId(): Int
    /**
     * Starts a branching, multi-step NPC conversation - npc/player chat lines, statements,
     * 2-5 option branches (which can themselves lead to further chat/options), and terminal
     * actions - built with [SkillDialogueFactory]. Use this instead of [playerOptionMenu] or
     * [itemListMenu] when the flow has more than one screen (e.g. an npc chat line, then an
     * option menu, then a confirm prompt depending on the choice).
     */
    fun dialogue(builder: SkillDialogueFactory.() -> Unit)
}

data class SkillItemGridEntry(val itemId: Int, val amount: Int)

/**
 * Queue/chain dialogue authoring API for plugins - a [SkillPlayer]-coupled mirror of the
 * engine's own `DialogueFactory` (game-server, `Client`-coupled, not importable by plugins).
 * A pure builder; runtime state is managed engine-side by [SkillUi.dialogue]'s implementation.
 * Intentionally omits the engine version's `restart` step - add it only when a real plugin
 * dialogue needs it.
 *
 * Emote ids are the engine's raw `DialogueEmote` values, hardcoded here since plugins can't
 * import that enum (game-server-owned) - same convention as other cross-boundary numeric ids
 * used elsewhere in this API (dialogue ids, interface ids). Common values: 591=DEFAULT,
 * 592=EVIL1, 596=DISTRESSED, 614=ANGRY1.
 */
class SkillDialogueFactory {
    /** Engine-only - the built step list, read by [SkillUi.dialogue]'s implementation. */
    val steps: MutableList<SkillDialogueStep> = mutableListOf()

    fun npcChat(npcId: Int, emote: Int = 591, vararg lines: String) {
        steps += SkillDialogueStep.NpcChat(npcId, emote, lines.joinToString("\n"))
    }

    fun playerChat(emote: Int = 591, vararg lines: String) {
        steps += SkillDialogueStep.PlayerChat(emote, lines.joinToString("\n"))
    }

    fun statement(vararg lines: String) {
        steps += SkillDialogueStep.Statement(lines.toList())
    }

    fun options(title: String = "Select an option", vararg options: SkillDialogueOption) {
        require(options.size in 2..5) { "Options must be 2..5 (got ${options.size})" }
        val built = options.map { opt -> SkillDialogueStep.Option(opt.text, buildSteps(opt.build)) }
        steps += SkillDialogueStep.Options(title, built)
    }

    fun action(action: (SkillPlayer) -> Unit) {
        steps += SkillDialogueStep.Action(action)
    }

    fun finish(closeInterfaces: Boolean = true, action: (SkillPlayer) -> Unit = {}) {
        steps += SkillDialogueStep.Finish(closeInterfaces, action)
    }

    fun finishThen(closeInterfaces: Boolean = true, action: (SkillPlayer) -> Unit = {}) {
        steps += SkillDialogueStep.FinishThen(closeInterfaces, action)
    }

    private fun buildSteps(builder: SkillDialogueFactory.() -> Unit): List<SkillDialogueStep> =
        SkillDialogueFactory().apply(builder).steps.toList()
}

data class SkillDialogueOption(val text: String, val build: SkillDialogueFactory.() -> Unit)

sealed interface SkillDialogueStep {
    data class NpcChat(val npcId: Int, val emote: Int, val text: String) : SkillDialogueStep
    data class PlayerChat(val emote: Int, val text: String) : SkillDialogueStep
    data class Statement(val lines: List<String>) : SkillDialogueStep
    data class Options(val title: String, val options: List<Option>) : SkillDialogueStep
    data class Option(val text: String, val steps: List<SkillDialogueStep>)
    data class Action(val action: (SkillPlayer) -> Unit) : SkillDialogueStep
    data class Finish(val closeInterfaces: Boolean, val action: (SkillPlayer) -> Unit) : SkillDialogueStep
    data class FinishThen(val closeInterfaces: Boolean, val action: (SkillPlayer) -> Unit) : SkillDialogueStep
}

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
     * Plays graphic [id] attached to the player's own model (follows them, e.g. a spell-cast
     * visual) - distinct from [graphic], which is a positional/ground-level effect.
     */
    fun castGraphic(id: Int, height: Int = 0)
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
    /** Assigns a new task, replacing whatever (if anything) was previously assigned. */
    fun assign(masterNpcId: Int, ordinal: Int, amount: Int)
    /** Clears the current task's remaining count to 0 (the task is now considered inactive). */
    fun cancel()
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
    /** Prayer-bonus equipment stat (drain-resistance), used to compute prayer drain rate. */
    val prayerBonus: Int
    /** Whether the player is currently in a duel fight - prayer toggling is blocked here. */
    val inDuel: Boolean
    /** Whether the player is in the death sequence - prayer toggling is blocked here. */
    val isDead: Boolean
    /**
     * Whether a prayer is currently active.  Skill modules use this stable
     * identifier instead of reaching into the engine's prayer manager.
     */
    fun isPrayerActive(prayer: SkillPrayer): Boolean
    /**
     * Raw prayer state flip (sets the on/off state, writes the client varbit, and updates the
     * overhead-prayer head icon if applicable) - does NOT validate level/points/duel/death or
     * handle the mutual-exclusion cascade between conflicting prayers. Callers (skill modules)
     * own that decision-making; this is only the low-level engine-side consequence of "yes,
     * flip this one prayer."
     */
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
class SkillItemOnNpcInteraction(val player: SkillPlayer, val npc: SkillNpcRef, val itemId: Int, val itemSlot: Int) {
    val npcId get() = npc.id
}
class SkillMagicOnItemInteraction(val player: SkillPlayer, val itemId: Int, val itemSlot: Int, val spellId: Int)
class SkillButtonInteraction(val player: SkillPlayer, val rawButtonId: Int, val opIndex: Int, val activeInterfaceId: Int)
class SkillItemGridInteraction(val player: SkillPlayer, val interfaceId: Int, val itemId: Int, val slot: Int, val amount: Int)
