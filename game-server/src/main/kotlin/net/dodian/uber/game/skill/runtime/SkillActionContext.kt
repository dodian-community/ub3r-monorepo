package net.dodian.uber.game.skill.runtime

import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client

data class SkillActionContext(
    val player: Client,
    val objectId: Int,
    val option: Int,
    val objectPosition: Position,
)
