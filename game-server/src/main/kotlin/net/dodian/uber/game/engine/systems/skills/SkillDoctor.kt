package net.dodian.uber.game.engine.systems.skills

import net.dodian.uber.game.skill.runtime.parity.SkillDoctorReport

object SkillDoctor {
    @JvmStatic
    fun snapshot(): SkillDoctorReport = net.dodian.uber.game.skill.runtime.parity.SkillDoctor.snapshot()

    @JvmStatic
    fun validateOrThrow() {
        net.dodian.uber.game.skill.runtime.parity.SkillDoctor.validateOrThrow()
    }
}

