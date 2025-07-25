package net.dodian.uber.game.netty.listener;

import net.dodian.uber.game.model.entity.player.Client;

/**
 * Modern Netty-based outgoing packet interface for pure Netty implementation.
 * Designed for performance and simplicity with zero-copy principles.
 * 
 * All outgoing packets should implement this interface and create ByteMessage
 * objects to send through the pure Netty pipeline.
 */
@FunctionalInterface
public interface OutgoingPacket {
    
    /**
     * Sends this packet to the specified client using pure Netty.
     * Implementations should create ByteMessage objects and call client.send(ByteMessage).
     * 
     * Example implementation:
     * <pre>
     * public void send(Client client) {
     *     ByteMessage message = ByteMessage.message(126, MessageType.VAR_SHORT);
     *     message.putString("Hello World");
     *     message.putShort(12345, ValueType.ADD);
     *     client.send(message);
     * }
     * </pre>
     * 
     * @param client The client to send the packet to
     */
    void send(Client client);
    
    /**
     * Optional method to get packet priority for potential future queue optimization.
     * Higher values = higher priority. Default implementation returns normal priority.
     * 
     * @return The priority level (0 = low, 1 = normal, 2 = high, 3 = critical)
     */
    default int getPriority() {
        return 1; // Normal priority by default
    }
    
    /**
     * Optional method to indicate if this packet can be batched with others.
     * Used for potential future optimizations to batch multiple small packets.
     * 
     * @return true if this packet can be batched, false otherwise
     */
    default boolean isBatchable() {
        return false; // Most packets are not batchable by default
    }
    
    /**
     * Optional method to get an estimated size hint for buffer pre-allocation.
     * Implementations can override this to provide better buffer sizing.
     * This helps reduce memory allocations and improve performance.
     * 
     * @return Estimated packet size in bytes, or -1 if unknown
     */
    default int getEstimatedSize() {
        return -1; // Unknown size by default
    }
}