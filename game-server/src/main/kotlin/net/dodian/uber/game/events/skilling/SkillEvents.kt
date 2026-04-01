package net.dodian.uber.game.events.skilling

import net.dodian.uber.game.engine.event.GameEvent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.systems.skills.SkillProgressionMode
import net.dodian.uber.game.systems.skills.ActionStopReason

data class SkillActionStartEvent(
    val client: Client,
    val actionName: String,
) : GameEvent

data class SkillActionInterruptEvent(
    val client: Client,
    val actionName: String,
    val reason: ActionStopReason,
) : GameEvent

data class SkillActionCompleteEvent(
    val client: Client,
    val actionName: String,
) : GameEvent

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
