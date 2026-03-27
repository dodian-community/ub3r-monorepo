package net.dodian.uber.game.content.skills.core.runtime

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.content.skills.core.progression.SkillProgressionService

fun Client.sendFilterMessage(message: String) {
    send(SendMessage(message))
}

fun Client.addSkillExperience(skill: Skill, amount: Int) {
    SkillProgressionService.gainXp(this, amount, skill)
}

fun Client.playAnimation(animationId: Int) {
    requestAnim(animationId, 0)
}
