package net.dodian.uber.game.skill.runtime.parity

import net.dodian.uber.game.api.plugin.ContentBootstrap

object SkillDoctorBootstrap : ContentBootstrap {
    override val id: String = "skills.doctor"

    override fun bootstrap() {
        SkillDoctor.validateOrThrow()
    }
}
