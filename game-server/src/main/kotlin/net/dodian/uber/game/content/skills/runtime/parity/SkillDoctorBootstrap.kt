package net.dodian.uber.game.content.skills.runtime.parity

import net.dodian.uber.game.systems.plugin.ContentBootstrap

object SkillDoctorBootstrap : ContentBootstrap {
    override val id: String = "skills.doctor"

    override fun bootstrap() {
        SkillDoctor.validateOrThrow()
    }
}
