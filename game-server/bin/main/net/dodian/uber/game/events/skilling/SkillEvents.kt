package net.dodian.uber.game.events.skilling

import net.dodian.uber.game.events.GameEvent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.systems.skills.SkillProgressionMode
import net.dodian.uber.game.systems.skills.action.ActionStopReason

// ── Plugin-framework level ────────────────────────────────────────────────────

/** Fired when the skill plugin framework starts a new action for a player. */
data class SkillActionStartEvent(
    val client: Client,
    val actionName: String,
) : GameEvent

/** Fired when the skill plugin framework interrupts a player's action. */
data class SkillActionInterruptEvent(
    val client: Client,
    val actionName: String,
    val reason: ActionStopReason,
) : GameEvent

/** Fired when the skill plugin framework marks a player's action as complete. */
data class SkillActionCompleteEvent(
    val client: Client,
    val actionName: String,
) : GameEvent

/** Fired when XP is applied to a player's skill. */
data class SkillProgressAppliedEvent(
    val client: Client,
    val skill: Skill,
    val mode: SkillProgressionMode,
    val oldExperience: Int,
    val newExperience: Int,
    val oldLevel: Int,
    val newLevel: Int,
    val appliedAmount: Int,
) : GameEvent

// ── GatheringTask / action-cycle level ───────────────────────────────────────

/** Fired when a skilling action task is started at the loop level. */
data class SkillingActionStartedEvent(
    val client: Client,
    val actionName: String,
) : GameEvent

/** Fired on each tick of a running skilling action. */
data class SkillingActionCycleEvent(
    val client: Client,
    val actionName: String,
) : GameEvent

/** Fired when a skilling action cycle produces a successful result (e.g. ore mined). */
data class SkillingActionSucceededEvent(
    val client: Client,
    val actionName: String,
) : GameEvent

/** Fired when a skilling action task stops for any reason. */
data class SkillingActionStoppedEvent(
    val client: Client,
    val actionName: String,
    val reason: ActionStopReason,
) : GameEvent
