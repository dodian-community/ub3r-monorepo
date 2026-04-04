package net.dodian.uber.game.systems.skills

import net.dodian.uber.game.systems.content.ContentBootstrap

object SkillZDoctorBootstrap : ContentBootstrap {
    override val id: String = "skills.doctor"

    override fun bootstrap() {
        SkillDoctor.validateOrThrow()
    }
}
