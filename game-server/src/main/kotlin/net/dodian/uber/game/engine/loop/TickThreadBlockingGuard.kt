package net.dodian.uber.game.engine.loop

object TickThreadBlockingGuard {
    @JvmStatic
    fun requireNotGameThread(context: String) {
        check(!GameThreadContext.isGameThread()) { "Blocking call on game thread: $context" }
    }
}
