package net.dodian.uber.game.engine.systems.skills

import net.dodian.uber.game.content.skills.runtime.parity.SkillDoctorReport

object SkillDoctor {
    @JvmStatic
    fun snapshot(): SkillDoctorReport = net.dodian.uber.game.content.skills.runtime.parity.SkillDoctor.snapshot()

    @JvmStatic
    fun validateOrThrow() {
        net.dodian.uber.game.content.skills.runtime.parity.SkillDoctor.validateOrThrow()
    }
}

