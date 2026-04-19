package net.dodian.uber.game.engine.systems.skills

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage

fun Client.sendFilterMessage(message: String) {
    send(SendMessage(message))
}

fun Client.addSkillExperience(skill: Skill, amount: Int) {
    ProgressionService.addXp(this, amount, skill)
}

fun Client.playAnimation(animationId: Int) {
    performAnimation(animationId, 0)
}
