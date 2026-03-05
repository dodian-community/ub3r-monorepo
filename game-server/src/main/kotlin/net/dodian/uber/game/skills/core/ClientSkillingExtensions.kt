package net.dodian.uber.game.skills.core

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage

fun Client.sendFilterMessage(message: String) {
    send(SendMessage(message))
}

fun Client.addSkillExperience(skill: Skill, amount: Int) {
    giveExperience(amount, skill)
}

fun Client.playAnimation(animationId: Int) {
    requestAnim(animationId, 0)
}
