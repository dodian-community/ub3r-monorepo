package net.dodian.uber.game.runtime.net

import io.netty.channel.Channel
import java.util.ArrayDeque
import net.dodian.uber.game.netty.codec.ByteMessage

/**
 * Per-client queued outbound transport. Messages are appended in call order and
 * drained once per tick.
 */
class OutboundSessionQueue {
    class DrainResult private constructor(
        private val messageCount: Int,
        private val byteCount: Int,
    ) {
        fun messageCount(): Int = messageCount

        fun byteCount(): Int = byteCount

        companion object {
            private val EMPTY = DrainResult(0, 0)

            @JvmStatic
            fun empty(): DrainResult = EMPTY

            @JvmStatic
            fun of(messageCount: Int, byteCount: Int): DrainResult =
                if (messageCount == 0 && byteCount == 0) {
                    EMPTY
                } else {
                    DrainResult(messageCount, byteCount)
                }
        }
    }

    private val queuedMessages = ArrayDeque<ByteMessage>()

    @Synchronized
    fun enqueue(message: ByteMessage) {
        queuedMessages.addLast(message)
    }

    @Synchronized
    fun isEmpty(): Boolean = queuedMessages.isEmpty()

    @Synchronized
    fun drainTo(channel: Channel): DrainResult {
        if (queuedMessages.isEmpty()) {
            return DrainResult.empty()
        }
        var messages = 0
        var bytes = 0
        while (!queuedMessages.isEmpty()) {
            val message = queuedMessages.removeFirst()
            bytes += maxOf(0, message.content().writerIndex())
            channel.write(message)
            messages++
        }
        return DrainResult.of(messages, bytes)
    }

    @Synchronized
    fun releaseAll() {
        while (!queuedMessages.isEmpty()) {
            queuedMessages.removeFirst().releaseAll()
        }
    }
}
