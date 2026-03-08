package net.dodian.uber.game.runtime.action

data class PlayerActionInterruptPolicy(
    val cancelOnMove: Boolean = false,
    val cancelOnCombat: Boolean = true,
    val cancelOnDialogue: Boolean = true,
) {
    companion object {
        @JvmField
        val DEFAULT = PlayerActionInterruptPolicy()

        @JvmField
        val TELEPORT = PlayerActionInterruptPolicy(cancelOnMove = false, cancelOnCombat = true, cancelOnDialogue = false)
    }
}
