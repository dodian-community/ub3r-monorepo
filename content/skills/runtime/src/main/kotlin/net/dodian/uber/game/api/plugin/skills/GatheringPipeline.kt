package net.dodian.uber.game.api.plugin.skills

import net.dodian.uber.game.api.content.ContentAttributeKey
import net.dodian.uber.game.engine.systems.action.PolicyPreset
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.skill.runtime.action.ActionStopReason
import net.dodian.uber.game.skill.runtime.action.CycleSignal
import net.dodian.uber.game.skill.runtime.action.gatheringAction

private const val TICK_MS = 600L

private fun toTicks(ms: Long): Int = if (ms <= 0L) 1 else ((ms + TICK_MS - 1) / TICK_MS).toInt().coerceAtLeast(1)

internal enum class GatheringTargetKind { NPC, OBJECT }

/**
 * Attribute keys for one gathering pipeline instance, namespaced by [actionName] so
 * multiple gathering skills (fishing, mining, woodcutting, ...) built on this same
 * framework never collide in a player's shared plugin-attribute store.
 */
private class GatheringKeys(actionName: String) {
    private val owner = "skill.$actionName"
    val gathered = ContentAttributeKey<Int>(owner, "gathered")
    val ticksUntilYield = ContentAttributeKey<Int>(owner, "ticksUntilYield")
    val ticksUntilAnimation = ContentAttributeKey<Int>(owner, "ticksUntilAnimation")
    val activeAction = ContentAttributeKey<SkillActionHandle>(owner, "activeAction")
    val targetRef = ContentAttributeKey<SkillObjectRef>(owner, "targetRef")
}

/**
 * Declarative "click a spot, then repeatedly roll for a timed yield" pipeline shared
 * by fishing/mining/woodcutting-shaped skills. Requirements are checked both before
 * starting and on every cycle (mirroring GatheringTask's own per-tick re-validation),
 * so content authors describe the rules once instead of duplicating them in an
 * `attempt()` guard clause AND an inner per-tick loop.
 */
class GatheringSpotBuilder<T> internal constructor(val spot: T) {
    internal var targetKind: GatheringTargetKind = GatheringTargetKind.NPC
    var targetId: Int = -1
        private set
    var clickOption: Int = 1
        private set
    internal var preset: PolicyPreset = PolicyPreset.GATHERING
    internal var trackBoundary: Boolean = false

    internal var animationId: Int = -1
    internal var animationIntervalMs: Long = 1800L
    private var delayCalculator: (SkillPlayer) -> Long = { 1L }
    private var altToolCheck: ((SkillPlayer) -> Boolean)? = null
    private val requirements = mutableListOf<Pair<(SkillPlayer) -> Boolean, (SkillPlayer) -> Unit>>()
    internal val consumables = mutableListOf<Pair<Int, Int>>()
    private var startHandler: ((SkillPlayer) -> Unit)? = null
    private var yieldHandler: ((SkillPlayer) -> Unit)? = null
    private var restMinGathered: Int = Int.MAX_VALUE
    private var restChanceDenominator: Int = 1

    /** Which npc + click option (e.g. "Net", "Bait") starts this spot. */
    fun npcOption(npcId: Int, clickOption: Int, preset: PolicyPreset = PolicyPreset.GATHERING) {
        targetKind = GatheringTargetKind.NPC
        this.targetId = npcId
        this.clickOption = clickOption
        this.preset = preset
    }

    /**
     * Which object + click option (e.g. tree/rock, option 1) starts this spot. Unlike
     * npc spots, object spots are automatically re-validated every cycle against
     * [SkillWorld.withinObjectBoundary] — walking away from the specific tree/rock
     * clicked stops the action, matching how object-anchored gathering always worked.
     */
    fun objectOption(objectId: Int, clickOption: Int, preset: PolicyPreset = PolicyPreset.GATHERING) {
        targetKind = GatheringTargetKind.OBJECT
        this.targetId = objectId
        this.clickOption = clickOption
        this.preset = preset
        trackBoundary = true
    }

    fun requireLevel(skill: Skill, level: Int, message: (SkillPlayer) -> String) {
        requirement({ it.skills.current(skill) >= level }, message)
    }

    fun requireFreeInventory(message: String) {
        requirement({ it.inventory.freeSlots() > 0 }, { message })
    }

    fun requirePremium(required: Boolean, message: (SkillPlayer) -> String) {
        if (!required) return
        requirement({ it.profile.premium }, message)
    }

    /** The primary tool. Combined with any [altTool] check registered alongside it. */
    fun requireTool(itemId: Int, message: (SkillPlayer) -> String) {
        requirement({ p -> p.inventory.contains(itemId) || altToolCheck?.invoke(p) == true }, message)
    }

    /** An alternative to the primary tool (e.g. a harpoon that replaces a net/rod). */
    fun altTool(check: (SkillPlayer) -> Boolean) {
        altToolCheck = check
    }

    /** Requires and, on each successful yield, consumes [amount] of [itemId] (e.g. feathers, bait). */
    fun requireConsumable(itemId: Int, amount: Int = 1, enabled: Boolean = true, message: (SkillPlayer) -> String) {
        if (!enabled) return
        requirement({ it.inventory.contains(itemId, amount) }, message)
        consumables += itemId to amount
    }

    fun requirement(check: (SkillPlayer) -> Boolean, message: (SkillPlayer) -> String) {
        requirements += (check to { p: SkillPlayer -> p.ui.message(message(p)) })
    }

    /** Re-triggers [animationId] roughly every [intervalMs] while the action runs. */
    fun repeatAnimation(animationId: Int, intervalMs: Long = 1800L) {
        this.animationId = animationId
        this.animationIntervalMs = intervalMs
    }

    fun calculateDelayMs(calculator: (SkillPlayer) -> Long) {
        delayCalculator = calculator
    }

    fun onStart(handler: (SkillPlayer) -> Unit) {
        startHandler = handler
    }

    /** Runs once per successful roll; consumables are already removed by the time this fires. */
    fun onYield(handler: (SkillPlayer) -> Unit) {
        yieldHandler = handler
    }

    /** After [minGathered] yields, there's a 1-in-[chanceOneIn] chance to stop and rest. */
    fun restPolicy(minGathered: Int, chanceOneIn: Int) {
        restMinGathered = minGathered
        restChanceDenominator = chanceOneIn
    }

    internal fun validate(player: SkillPlayer): Boolean {
        for ((check, onFailure) in requirements) {
            if (!check(player)) {
                onFailure(player)
                return false
            }
        }
        return true
    }

    /**
     * Validates and, if successful, starts this spot's gathering action for [player].
     * [targetRef] is required for object spots (the specific instance clicked, used
     * for the automatic boundary check) and ignored for npc spots.
     */
    fun start(player: SkillPlayer, actionName: String, targetRef: SkillObjectRef? = null) {
        if (!validate(player)) return
        val keys = GatheringKeys(actionName)
        player.attributes.get(keys.activeAction)?.cancel(ActionStopReason.USER_INTERRUPT)

        player.attributes.put(keys.gathered, 0)
        player.attributes.put(keys.ticksUntilYield, toTicks(delayCalculator(player)))
        if (animationId >= 0) {
            player.attributes.put(keys.ticksUntilAnimation, toTicks(animationIntervalMs))
            player.actions.animate(animationId, 0)
        }
        if (trackBoundary && targetRef != null) {
            player.attributes.put(keys.targetRef, targetRef)
        }
        startHandler?.invoke(player)

        val handle = gatheringAction(actionName) {
            delay(1)
            onCycleSignal { cycleOnce(this, keys) }
            onStop {
                player.attributes.remove(keys.gathered)
                player.attributes.remove(keys.ticksUntilYield)
                player.attributes.remove(keys.ticksUntilAnimation)
                player.attributes.remove(keys.targetRef)
                player.attributes.remove(keys.activeAction)
            }
        }.start(player)

        if (handle == null) {
            player.attributes.remove(keys.gathered)
            player.attributes.remove(keys.ticksUntilYield)
            player.attributes.remove(keys.ticksUntilAnimation)
            player.attributes.remove(keys.targetRef)
            return
        }
        player.attributes.put(keys.activeAction, handle)
    }

    private fun cycleOnce(player: SkillPlayer, keys: GatheringKeys): CycleSignal {
        val trackedTarget = player.attributes.get(keys.targetRef)
        if (trackedTarget != null && !player.world.withinObjectBoundary(trackedTarget)) {
            player.ui.message("You moved too far away.")
            return CycleSignal.stop()
        }
        if (!validate(player)) return CycleSignal.stop()

        if (animationId >= 0) {
            val ticksUntilAnimation = (player.attributes.get(keys.ticksUntilAnimation) ?: 0) - 1
            if (ticksUntilAnimation <= 0) {
                player.actions.animate(animationId, 0)
                player.attributes.put(keys.ticksUntilAnimation, toTicks(animationIntervalMs))
            } else {
                player.attributes.put(keys.ticksUntilAnimation, ticksUntilAnimation)
            }
        }

        val ticksUntilYield = (player.attributes.get(keys.ticksUntilYield) ?: 0) - 1
        if (ticksUntilYield > 0) {
            player.attributes.put(keys.ticksUntilYield, ticksUntilYield)
            return CycleSignal.continueWithoutSuccess()
        }

        val committed = consumables.isEmpty() || player.inventory.transaction {
            consumables.forEach { (itemId, amount) -> remove(itemId, amount) }
        }
        if (!committed) return CycleSignal.stop(ActionStopReason.REQUIREMENT_FAILED)

        yieldHandler?.invoke(player)

        val gathered = (player.attributes.get(keys.gathered) ?: 0) + 1
        player.attributes.put(keys.gathered, gathered)
        player.attributes.put(keys.ticksUntilYield, toTicks(delayCalculator(player)))
        if (animationId >= 0) player.attributes.put(keys.ticksUntilAnimation, toTicks(animationIntervalMs))

        if (gathered >= restMinGathered && player.random.chance(1, restChanceDenominator)) {
            player.ui.message("You take a rest after gathering $gathered resources.")
            return CycleSignal.stop(ActionStopReason.COMPLETED)
        }
        return CycleSignal.success()
    }
}

/**
 * Registers one [GatheringSpotBuilder] per entry in [spots] and auto-binds the
 * resulting npc/object clicks, grouped by (target kind, click option) so spots
 * sharing an option (e.g. every "Net" fishing spot, every tree tier) share a single
 * [SkillPluginBuilder] binding — the same shape every gathering skill previously
 * hand-wrote with groupBy/distinct.
 */
fun <T> SkillPluginBuilder.gatheringSpots(
    spots: List<T>,
    actionName: String,
    configure: GatheringSpotBuilder<T>.(T) -> Unit,
): List<GatheringSpotBuilder<T>> {
    val builders = spots.map { spot -> GatheringSpotBuilder(spot).apply { configure(spot) } }
    for ((key, group) in builders.groupBy { it.targetKind to it.clickOption }) {
        val (kind, option) = key
        val preset = group.first().preset
        val ids = group.map { it.targetId }.distinct().toIntArray()
        if (ids.isEmpty()) continue
        when (kind) {
            GatheringTargetKind.NPC ->
                npcClick(preset = preset, option = option, *ids) { interaction ->
                    val builder = group.firstOrNull { it.targetId == interaction.npc.id } ?: return@npcClick false
                    builder.start(interaction.player, actionName)
                    true
                }
            GatheringTargetKind.OBJECT ->
                objectClick(preset = preset, option = option, *ids) { interaction ->
                    val builder = group.firstOrNull { it.targetId == interaction.objectId } ?: return@objectClick false
                    builder.start(interaction.player, actionName, interaction.target)
                    true
                }
        }
    }
    return builders
}

/** Cancels an in-progress [gatheringSpots]-built action for [actionName], if any. */
fun stopGathering(player: SkillPlayer, actionName: String, reason: ActionStopReason = ActionStopReason.USER_INTERRUPT) {
    val key = GatheringKeys(actionName).activeAction
    player.attributes.get(key)?.cancel(reason)
    player.attributes.remove(key)
}
