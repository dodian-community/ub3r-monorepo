package net.dodian.uber.game.systems.action

data class DuelCountdownState(
    val rawCounter: Int,
    val done: Boolean,
) {
    companion object {
        @JvmStatic
        fun initial(): DuelCountdownState = DuelCountdownState(rawCounter = 7, done = false)
    }
}

data class DuelCountdownStep(
    val nextState: DuelCountdownState,
    val forceChat: String?,
    val enableCombat: Boolean,
)

object DuelCountdownService {
    @JvmStatic
    fun advance(state: DuelCountdownState): DuelCountdownStep {
        if (state.done) {
            return DuelCountdownStep(nextState = state, forceChat = null, enableCombat = false)
        }

        val nextCounter = state.rawCounter - 1
        if (nextCounter < 1) {
            return DuelCountdownStep(
                nextState = DuelCountdownState(rawCounter = nextCounter, done = true),
                forceChat = "Fight!",
                enableCombat = true,
            )
        }

        val forceChat = if (nextCounter % 2 == 0) "${nextCounter / 2}" else null
        return DuelCountdownStep(
            nextState = DuelCountdownState(rawCounter = nextCounter, done = false),
            forceChat = forceChat,
            enableCombat = false,
        )
    }
}
