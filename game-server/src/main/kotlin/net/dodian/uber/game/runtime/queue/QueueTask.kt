package net.dodian.uber.game.runtime.queue

fun interface QueueTask {
    fun execute(): Boolean
}
