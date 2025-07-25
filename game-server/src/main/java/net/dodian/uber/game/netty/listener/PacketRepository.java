package net.dodian.uber.game.netty.listener;

import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkState;


public final class PacketRepository implements Iterable<PacketListener> {

    private static final Logger logger = LoggerFactory.getLogger(PacketRepository.class);

    /**
     * Maximum number of opcodes (0-255).
     */
    private static final int MAX_OPCODES = 256;

    /**
     * Array storing packet listeners indexed by opcode.
     */
    private final PacketListener[] listeners = new PacketListener[MAX_OPCODES];

    /**
     * Whether this repository is locked (read-only).
     */
    private volatile boolean locked = false;

    /**
     * Singleton instance.
     */
    private static final PacketRepository INSTANCE = new PacketRepository();

    private PacketRepository() {
        // Private constructor for singleton pattern
    }

    /**
     * Gets the singleton instance of the packet repository.
     */
    public static PacketRepository getInstance() {
        return INSTANCE;
    }

    /**
     * Registers a packet listener for the specified opcode.
     *
     * @param opcode The opcode to register the listener for
     * @param listener The packet listener to register
     * @throws IllegalStateException If the repository is locked
     * @throws IllegalArgumentException If opcode is out of range or listener already exists
     */
    public void register(int opcode, PacketListener listener) {
        checkState(!locked, "Repository is locked and cannot be modified");
        
        if (opcode < 0 || opcode >= MAX_OPCODES) {
            throw new IllegalArgumentException("Opcode out of range: " + opcode + " (must be 0-255)");
        }
        
        if (listeners[opcode] != null) {
            // Only warn if it's actually a different listener instance/class
            if (listeners[opcode] != listener && !listeners[opcode].getClass().equals(listener.getClass())) {
                logger.warn("Overwriting existing listener for opcode {}: {} -> {}", 
                    opcode, listeners[opcode].getClass().getSimpleName(), listener.getClass().getSimpleName());
            } else {
                logger.debug("Re-registering same listener for opcode {}: {}", 
                    opcode, listener.getClass().getSimpleName());
            }
        }
        
        listeners[opcode] = listener;
        logger.debug("Registered {} for opcode {}", listener.getClass().getSimpleName(), opcode);
    }

    /**
     * Retrieves the packet listener for the specified opcode.
     *
     * @param opcode The opcode to get the listener for
     * @return The packet listener, or null if none exists
     */
    public PacketListener get(int opcode) {
        if (opcode < 0 || opcode >= MAX_OPCODES) {
            return null;
        }
        return listeners[opcode];
    }

    /**
     * Checks if a listener exists for the specified opcode.
     *
     * @param opcode The opcode to check
     * @return true if a listener exists, false otherwise
     */
    public boolean has(int opcode) {
        return get(opcode) != null;
    }

    /**
     * Registers a no-op listener for the specified opcode.
     * Useful for consuming packets that should be ignored.
     *
     * @param opcode The opcode to register a no-op listener for
     */
    public void registerNoOp(int opcode) {
        register(opcode, (client, packet) -> {
            // Consume the packet payload to prevent unhandled packet warnings
            packet.getPayload().skipBytes(packet.getPayload().readableBytes());
        });
    }

    /**
     * Locks this repository, making it read-only.
     * This should be called after all listeners are registered.
     */
    public void lock() {
        if (!locked) {
            locked = true;
            logger.info("Packet repository locked with {} registered listeners", getRegisteredCount());
        }
    }

    /**
     * Gets the number of registered listeners.
     *
     * @return The count of non-null listeners
     */
    public int getRegisteredCount() {
        int count = 0;
        for (PacketListener listener : listeners) {
            if (listener != null) {
                count++;
            }
        }
        return count;
    }

    /**
     * Checks if the repository is locked.
     *
     * @return true if locked, false otherwise
     */
    public boolean isLocked() {
        return locked;
    }

    @Override
    public UnmodifiableIterator<PacketListener> iterator() {
        return Iterators.forArray(listeners);
    }
}