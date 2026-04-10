package net.dodian.uber.game.engine.scheduler

fun interface QueueTask {
    fun execute(): Boolean
}
