package net.dodian.uber.game.runtime.scheduler

fun interface QueueTask {
    fun execute(): Boolean
}
