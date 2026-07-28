package net.dodian.uber.skills.testkit

import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.skill.runtime.action.CycleSignal
import net.dodian.uber.game.skill.runtime.action.productionAction

/** Shared capability/action contract for a declared Gradle skill module. */
object LiveSkillModuleFixture {
    fun requirePlugin(moduleId: String, skill: Skill) {
        check(moduleId == "skill.${skill.name.lowercase()}") { "Module id $moduleId does not match $skill" }
        // A module-specific route test supplies registry ownership. This shared
        // fixture verifies only the public capability/action contract.
        val input = 20_000 + skill.id
        val output = 30_000 + skill.id
        val player = FakeSkillPlayer(mapOf(input to 1)).apply { setLevel(skill, 99) }
        check(player.actions.beginSession(moduleId))
        val action = productionAction("fixture.$moduleId") {
            delay(1)
            onCycleSignal {
                if (!inventory.transaction { remove(input); add(output) }) {
                    return@onCycleSignal CycleSignal.stop()
                }
                CycleSignal.success()
            }
            onSuccess {
                skills.gainXp(1, skill)
                ui.message("$moduleId fixture complete")
                actions.endSession(moduleId)
            }
        }.start(player)
        check(action != null) { "$moduleId could not queue its capability fixture" }
        player.advanceTicks()
        check(player.amount(input) == 0 && player.amount(output) == 1) { "$moduleId inventory outcome failed" }
        check(player.skills.experience(skill) == 1) { "$moduleId XP outcome failed" }
        check(player.messages.singleOrNull() == "$moduleId fixture complete") { "$moduleId message outcome failed" }
        check(player.actions.activeSessionKey() == null) { "$moduleId session cleanup failed" }
    }
}
