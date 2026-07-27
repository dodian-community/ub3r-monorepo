package net.dodian.uber.quests.runtime

import net.dodian.uber.game.api.plugin.quests.QuestPlayer

/**
 * A single stage's setup/restore logic. Ports Tutorial Island's
 * `TutorialStage.load()` pattern: a stage handler must fully re-establish
 * whatever UI/world state that stage needs (teleport, tab visibility, current
 * objective message), so dispatching the same stage twice is always safe -
 * that's what makes resuming after logout just "dispatch the current stage
 * again." [resuming] is `true` on that login-resume path and `false` right
 * after [net.dodian.uber.game.api.plugin.quests.Quest.advanceQuestStage] moves
 * the player onto this stage, in case a stage wants to skip a one-time
 * intro/animation on resume.
 */
fun interface QuestStageHandler {
    fun handle(player: QuestPlayer, stage: Int, resuming: Boolean)
}

/**
 * Dispatches to a per-stage handler by stage number. Stage *storage* belongs to
 * [net.dodian.uber.game.api.plugin.quests.Quest]/`QuestProgress` - this only
 * runs the presentation/world-state side effects for a given stage number.
 */
class QuestStageMachine(private val stages: Map<Int, QuestStageHandler>) {
    fun dispatch(player: QuestPlayer, stage: Int, resuming: Boolean = false) {
        stages[stage]?.handle(player, stage, resuming)
    }
}
