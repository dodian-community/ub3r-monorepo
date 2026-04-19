package net.dodian.uber.game.systems.animation

enum class PlayerAnimationSource(val priority: Int) {
    RESET_CLEAR(0),
    SKILL_ACTION(10),
    BLOCK_REACTION(20),
    ATTACK(30),
    FORCED_SCRIPT(40),
}
