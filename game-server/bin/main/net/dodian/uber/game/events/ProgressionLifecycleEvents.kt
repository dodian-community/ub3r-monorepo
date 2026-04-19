package net.dodian.uber.game.events

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill

/** Fired when a player's skill level increases. */
data class LevelUpEvent(
    val client: Client,
    val skill: Skill,
    val oldLevel: Int,
    val newLevel: Int,
    val oldExperience: Int,
    val newExperience: Int,
) : GameEvent
