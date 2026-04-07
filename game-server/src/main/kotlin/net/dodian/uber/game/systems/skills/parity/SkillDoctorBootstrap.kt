package net.dodian.uber.game.systems.skills.parity

import net.dodian.uber.game.systems.dispatch.ContentBootstrap

object SkillDoctorBootstrap : ContentBootstrap {
    override val id: String = "skills.doctor"

    override fun bootstrap() {
        SkillDoctor.validateOrThrow()
    }
}
