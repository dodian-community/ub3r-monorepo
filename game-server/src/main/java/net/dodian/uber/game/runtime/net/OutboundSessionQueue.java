package net.dodian.uber.game.runtime.net;

import io.netty.channel.Channel;
import java.util.ArrayDeque;
import net.dodian.uber.game.netty.codec.ByteMessage;

/**
 * Per-client queued outbound transport. Messages are appended in call order and
 * drained once per tick.
 */
public final class OutboundSessionQueue {

    public static final class DrainResult {
        private final int messageCount;
        private final int byteCount;

        private DrainResult(int messageCount, int byteCount) {
            this.messageCount = messageCount;
            this.byteCount = byteCount;
        }

        public int messageCount() {
            return messageCount;
        }

        public int byteCount() {
            return byteCount;
        }

        public static DrainResult empty() {
            return new DrainResult(0, 0);
        }
    }

    private final ArrayDeque<ByteMessage> queuedMessages = new ArrayDeque<>();

    public synchronized void enqueue(ByteMessage message) {
        queuedMessages.addLast(message);
    }

    public synchronized boolean isEmpty() {
        return queuedMessages.isEmpty();
    }

    public synchronized DrainResult drainTo(Channel channel) {
        if (queuedMessages.isEmpty()) {
            return DrainResult.empty();
        }
        int messages = 0;
        int bytes = 0;
        while (!queuedMessages.isEmpty()) {
            ByteMessage message = queuedMessages.removeFirst();
            bytes += Math.max(0, message.content().writerIndex());
            channel.write(message);
            messages++;
        }
        return new DrainResult(messages, bytes);
    }

    public synchronized void releaseAll() {
        while (!queuedMessages.isEmpty()) {
            queuedMessages.removeFirst().releaseAll();
        }
    }
}
