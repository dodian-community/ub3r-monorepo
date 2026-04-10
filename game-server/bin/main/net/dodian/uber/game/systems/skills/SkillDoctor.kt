package net.dodian.uber.game.systems.skills

import net.dodian.uber.game.systems.skills.parity.SkillDoctorReport

object SkillDoctor {
    @JvmStatic
    fun snapshot(): SkillDoctorReport = net.dodian.uber.game.systems.skills.parity.SkillDoctor.snapshot()

    @JvmStatic
    fun validateOrThrow() {
        net.dodian.uber.game.systems.skills.parity.SkillDoctor.validateOrThrow()
    }
}

