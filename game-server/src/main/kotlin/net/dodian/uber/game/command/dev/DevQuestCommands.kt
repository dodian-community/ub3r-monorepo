package net.dodian.uber.game.command.dev

import net.dodian.uber.game.engine.systems.interaction.commands.CommandContent
import net.dodian.uber.game.engine.systems.interaction.commands.commands
import net.dodian.uber.game.engine.systems.quests.asQuestPlayer
import net.dodian.uber.quests.tutorialisland.TutorialIslandModule

/**
 * Staff-only entry point for testing Tutorial Island before it's wired into
 * new-character creation. `::tutorial` starts/resumes it at the current
 * stage; `::tutorial reset` clears progress back to not-started; `::tutorial
 * next` force-advances one stage without the real interaction - useful while
 * the exit-door object id and room coordinates are still unverified against a
 * live client (see TutorialIslandModule.kt / TutorialIslandNpcs.kt).
 *
 * On relogin, running `::tutorial` again resumes at the persisted stage -
 * proving `quest_data` round-trips through save/load. A real onboarding flow
 * would resume automatically on login instead of requiring this command; that
 * hook is intentionally not added here (see plan: opt-in scaffold only).
 */
object DevQuestCommands : CommandContent {
    override fun definitions() =
        commands {
            command("tutorial") {
                if (!specialRights) return@command false
                val player = client.asQuestPlayer()
                when (parts.getOrNull(1)?.lowercase()) {
                    "reset" -> {
                        player.questProgress.setStage(TutorialIslandModule.QUEST_KEY, 0)
                        client.sendMessage("Tutorial Island progress reset.")
                    }
                    "next" -> {
                        val stage = TutorialIslandModule.forceAdvance(player)
                        client.sendMessage("Tutorial Island force-advanced to stage $stage.")
                    }
                    else -> TutorialIslandModule.startOrResume(player)
                }
                true
            }
        }
}
